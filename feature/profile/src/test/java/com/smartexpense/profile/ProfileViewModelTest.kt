package com.smartexpense.profile

import app.cash.turbine.test
import com.smartexpense.domain.model.UserProfile
import com.smartexpense.domain.repository.UserDetails
import com.smartexpense.domain.usecase.AuthUseCase
import com.smartexpense.domain.usecase.ProfileUseCase
import com.smartexpense.profile.intent.ProfileIntent
import com.smartexpense.profile.viewmodel.ProfileEffect
import com.smartexpense.profile.viewmodel.ProfileViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private lateinit var profileUseCase: ProfileUseCase
    private lateinit var authUseCase: AuthUseCase
    private lateinit var testDispatcher: TestDispatcher

    private val profile = UserProfile(
        userId = "user-1",
        username = "Malvin",
        avatarUri = "https://example.com/avatar.jpg",
        email = "malvin@example.com",
        memberSince = "January 2024"
    )

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        profileUseCase = mockk()
        authUseCase = mockk()

        coEvery { profileUseCase.hydrateLocalFromRemote() } returns Unit
        coEvery { profileUseCase.observeLocalProfileForCurrentUser() } returns profile
        coEvery { profileUseCase.getCurrentUserDetails() } returns
                UserDetails(email = "malvin@example.com", memberSince = "January 2024")
        coEvery { profileUseCase.saveProfile(any(), any()) } returns Unit
        coEvery { authUseCase.signOut() } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ProfileViewModel(profileUseCase, authUseCase)

    @Test
    fun `initial load populates username email and member since`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("Malvin", state.username)
            assertEquals("malvin@example.com", state.email)
            assertEquals("https://example.com/avatar.jpg", state.avatarUri)
            assertFalse(state.isLoading)
            assertNull(state.error)
        }

    @Test
    fun `initial load hydrates local profile from remote`() =
        runTest(testDispatcher) {
            createViewModel()

            advanceUntilIdle()

            coVerify(exactly = 1) { profileUseCase.hydrateLocalFromRemote() }
        }

    @Test
    fun `initial load clears username when remote has no profile`() =
        runTest(testDispatcher) {
            coEvery { profileUseCase.observeLocalProfileForCurrentUser() } returns null

            val viewModel = createViewModel()

            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("", state.username)
            assertNull(state.avatarUri)
            assertFalse(state.isLoading)
        }

    @Test
    fun `initial load falls back to local data when hydration fails`() =
        runTest(testDispatcher) {
            coEvery { profileUseCase.hydrateLocalFromRemote() } throws
                    IllegalStateException("Network unavailable")

            val viewModel = createViewModel()

            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("Malvin", state.username)
            assertFalse(state.isLoading)
            assertNull(state.error)
        }

    @Test
    fun `initial load shows error when local profile read fails`() =
        runTest(testDispatcher) {
            coEvery { profileUseCase.getCurrentUserDetails() } throws
                    IllegalStateException("Failed to read account")

            val viewModel = createViewModel()

            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("Failed to read account", state.error)
            assertFalse(state.isLoading)
        }

    @Test
    fun `initial load falls back to local data when hydration times out`() =
        runTest(testDispatcher) {
            coEvery { profileUseCase.hydrateLocalFromRemote() } coAnswers {
                kotlinx.coroutines.delay(10_000)
            }

            val viewModel = createViewModel()

            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("Malvin", state.username)
            assertEquals("malvin@example.com", state.email)
            assertFalse(state.isLoading)
            assertNull(state.error)
        }

    @Test
    fun `username change updates state and saves profile`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onIntent(ProfileIntent.UsernameChanged("NewName"))
            advanceUntilIdle()

            assertEquals("NewName", viewModel.uiState.value.username)
            coVerify(exactly = 1) {
                profileUseCase.saveProfile("NewName", "https://example.com/avatar.jpg")
            }
        }

    @Test
    fun `avatar change updates state and saves profile`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onIntent(ProfileIntent.AvatarChanged("content://new-avatar"))
            advanceUntilIdle()

            assertEquals("content://new-avatar", viewModel.uiState.value.avatarUri)
            coVerify(exactly = 1) {
                profileUseCase.saveProfile("Malvin", "content://new-avatar")
            }
        }

    @Test
    fun `successful save shows temporary success message`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onIntent(ProfileIntent.SaveProfile)
            advanceTimeBy(500)

            assertEquals("Profile saved", viewModel.uiState.value.saveMessage)

            // The message is auto-dismissed after 1.5s.
            advanceUntilIdle()
            assertNull(viewModel.uiState.value.saveMessage)
            assertFalse(viewModel.uiState.value.isSaving)
        }

    @Test
    fun `failed save shows error and stops saving`() =
        runTest(testDispatcher) {
            coEvery { profileUseCase.saveProfile(any(), any()) } throws
                    IllegalStateException("Upload failed")

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onIntent(ProfileIntent.SaveProfile)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("Upload failed", state.error)
            assertFalse(state.isSaving)
        }

    @Test
    fun `sign out emits navigate to sign in`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.effect.test {
                viewModel.onIntent(ProfileIntent.SignOut)
                advanceUntilIdle()

                assertEquals(ProfileEffect.NavigateToSignIn, awaitItem())

                cancelAndIgnoreRemainingEvents()
            }

            coVerify(exactly = 1) { authUseCase.signOut() }
        }

    @Test
    fun `failed sign out emits error effect`() =
        runTest(testDispatcher) {
            coEvery { authUseCase.signOut() } throws
                    IllegalStateException("Sign out failed")

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.effect.test {
                viewModel.onIntent(ProfileIntent.SignOut)
                advanceUntilIdle()

                val effect = awaitItem()
                assertTrue(effect is ProfileEffect.ShowError)
                assertEquals(
                    "Sign out failed",
                    (effect as ProfileEffect.ShowError).message
                )

                cancelAndIgnoreRemainingEvents()
            }
        }
}
