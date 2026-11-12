package com.smartexpense.domain

import com.smartexpense.domain.model.UserProfile
import com.smartexpense.domain.repository.AuthRepository
import com.smartexpense.domain.repository.ProfileRemoteRepository
import com.smartexpense.domain.repository.ProfileRepository
import com.smartexpense.domain.repository.UserDetails
import com.smartexpense.domain.usecase.ProfileUseCase
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProfileUseCaseTest {

    private lateinit var profileRepository: ProfileRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var profileRemoteRepository: ProfileRemoteRepository
    private lateinit var useCase: ProfileUseCase

    @Before
    fun setUp() {
        profileRepository = mockk()
        authRepository = mockk()
        profileRemoteRepository = mockk()
        useCase = ProfileUseCase(
            profileRepository = profileRepository,
            authRepository = authRepository,
            profileRemoteRepository = profileRemoteRepository
        )
    }

    @Test
    fun `saveProfile persists to local store and remote`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { profileRepository.saveProfile(any(), any(), any()) } just Runs
        coEvery {
            profileRemoteRepository.saveProfile(any(), any(), any())
        } returns null

        useCase.saveProfile(
            username = "Malvin",
            avatarUri = "content://avatar"
        )

        coVerify(exactly = 1) {
            profileRepository.saveProfile("user-1", "Malvin", "content://avatar")
        }
        coVerify(exactly = 1) {
            profileRemoteRepository.saveProfile("Malvin", "content://avatar", false)
        }
    }

    @Test
    fun `saveProfile does nothing when no user is signed in`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns null

        useCase.saveProfile(username = "Malvin", avatarUri = null)

        coVerify(exactly = 0) {
            profileRepository.saveProfile(any(), any(), any())
        }
        coVerify(exactly = 0) {
            profileRemoteRepository.saveProfile(any(), any(), any())
        }
    }

    @Test
    fun `hydrateLocalFromRemote copies remote profile into local store`() =
        runTest {
            coEvery { authRepository.getCurrentUserId() } returns "user-1"
            coEvery { profileRemoteRepository.loadProfile() } returns
                    ProfileRemoteRepository.RemoteProfile(
                        username = "Malvin",
                        avatarUri = "https://cdn/avatar.jpg"
                    )
            coEvery { profileRepository.saveProfile(any(), any(), any()) } just Runs

            useCase.hydrateLocalFromRemote()

            coVerify(exactly = 1) {
                profileRepository.saveProfile(
                    "user-1",
                    "Malvin",
                    "https://cdn/avatar.jpg"
                )
            }
            coVerify(exactly = 0) { profileRepository.deleteProfile(any()) }
        }

    @Test
    fun `hydrateLocalFromRemote clears stale local profile when remote is empty`() =
        runTest {
            coEvery { authRepository.getCurrentUserId() } returns "user-2"
            coEvery { profileRemoteRepository.loadProfile() } returns null
            coEvery { profileRepository.deleteProfile(any()) } just Runs

            useCase.hydrateLocalFromRemote()

            coVerify(exactly = 1) { profileRepository.deleteProfile("user-2") }
            coVerify(exactly = 0) {
                profileRepository.saveProfile(any(), any(), any())
            }
        }

    @Test
    fun `hydrateLocalFromRemote does nothing when no user is signed in`() =
        runTest {
            coEvery { authRepository.getCurrentUserId() } returns null

            useCase.hydrateLocalFromRemote()

            coVerify(exactly = 0) { profileRemoteRepository.loadProfile() }
            coVerify(exactly = 0) { profileRepository.deleteProfile(any()) }
        }

    @Test
    fun `observeLocalProfileForCurrentUser returns local profile`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        every { profileRepository.observeProfile("user-1") } returns flowOf(
            UserProfile(
                userId = "user-1",
                username = "Malvin",
                avatarUri = "https://cdn/avatar.jpg"
            )
        )

        val profile = useCase.observeLocalProfileForCurrentUser()

        assertEquals("Malvin", profile?.username)
        assertEquals("user-1", profile?.userId)
    }

    @Test
    fun `observeLocalProfileForCurrentUser returns null when no user`() =
        runTest {
            coEvery { authRepository.getCurrentUserId() } returns null

            assertNull(useCase.observeLocalProfileForCurrentUser())

            verify(exactly = 0) { profileRepository.observeProfile(any()) }
        }

    @Test
    fun `getCurrentUserDetails delegates to auth repository`() = runTest {
        val details = UserDetails(
            email = "malvin@example.com",
            memberSince = "January 2024"
        )
        coEvery { authRepository.getCurrentUserDetails() } returns details

        assertEquals(details, useCase.getCurrentUserDetails())
    }

    @Test
    fun `saveProfile persists the uploaded public URL locally`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { profileRepository.saveProfile(any(), any(), any()) } just Runs
        coEvery {
            profileRemoteRepository.saveProfile(any(), any(), any())
        } returns "https://cdn/avatar.jpg"

        val result = useCase.saveProfile(
            username = "Malvin",
            avatarUri = "content://avatar"
        )

        assertEquals("https://cdn/avatar.jpg", result)
        coVerifyOrder {
            profileRepository.saveProfile("user-1", "Malvin", "content://avatar")
            profileRepository.saveProfile(
                "user-1",
                "Malvin",
                "https://cdn/avatar.jpg"
            )
        }
    }

    @Test
    fun `removeAvatar clears local avatar and deletes remote avatar`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { profileRepository.saveProfile(any(), any(), any()) } just Runs
        coEvery {
            profileRemoteRepository.saveProfile(any(), any(), any())
        } returns null

        useCase.removeAvatar(username = "Malvin")

        coVerifyOrder {
            profileRepository.saveProfile(
                "user-1",
                "Malvin",
                null as String?
            )
            profileRemoteRepository.saveProfile("Malvin", null, true)
        }
    }

    @Test
    fun `removeAvatar does nothing when no user is signed in`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns null

        useCase.removeAvatar(username = "Malvin")

        coVerify(exactly = 0) {
            profileRepository.saveProfile(any(), any(), any())
        }
        coVerify(exactly = 0) {
            profileRemoteRepository.saveProfile(any(), any(), any())
        }
    }

    @Test
    fun `deleteProfile clears local profile and remote avatar`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { profileRepository.deleteProfile(any()) } just Runs
        coEvery {
            profileRemoteRepository.saveProfile(any(), any(), any())
        } returns null

        useCase.deleteProfile()

        coVerifyOrder {
            profileRepository.deleteProfile("user-1")
            profileRemoteRepository.saveProfile(null, null, true)
        }
    }

    @Test
    fun `ensureSpendoraId returns the existing remote id`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { profileRemoteRepository.loadSpendoraId() } returns "spendora42"

        val result = useCase.ensureSpendoraId()

        assertEquals("spendora42", result)
        coVerify(exactly = 0) { profileRemoteRepository.saveSpendoraId(any()) }
    }

    @Test
    fun `ensureSpendoraId generates and saves a new id`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { profileRemoteRepository.loadSpendoraId() } returns null
        coEvery { profileRemoteRepository.saveSpendoraId(any()) } just Runs

        val result = useCase.ensureSpendoraId()

        assertTrue(result != null && Regex("spendora\\d+").matches(result))
        coVerify(exactly = 1) { profileRemoteRepository.saveSpendoraId(result!!) }
    }

    @Test
    fun `ensureSpendoraId does nothing when signed out`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns null

        assertNull(useCase.ensureSpendoraId())
        coVerify(exactly = 0) { profileRemoteRepository.loadSpendoraId() }
    }

    @Test
    fun `loadEmailVisibility returns the remote preference`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { profileRemoteRepository.loadEmailVisibility() } returns false

        assertEquals(false, useCase.loadEmailVisibility())
    }

    @Test
    fun `loadEmailVisibility returns null when signed out`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns null

        assertNull(useCase.loadEmailVisibility())
        coVerify(exactly = 0) { profileRemoteRepository.loadEmailVisibility() }
    }

    @Test
    fun `saveEmailVisibility delegates when signed in`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { profileRemoteRepository.saveEmailVisibility(any()) } just Runs

        useCase.saveEmailVisibility(false)

        coVerify(exactly = 1) { profileRemoteRepository.saveEmailVisibility(false) }
    }

    @Test
    fun `saveEmailVisibility does nothing when signed out`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns null

        useCase.saveEmailVisibility(false)

        coVerify(exactly = 0) {
            profileRemoteRepository.saveEmailVisibility(any())
        }
    }
}
