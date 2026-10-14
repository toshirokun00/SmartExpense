package com.smartexpense.auth

import app.cash.turbine.test
import com.smartexpense.auth.effect.AuthEffect
import com.smartexpense.auth.intent.AuthIntent
import com.smartexpense.auth.viewmodel.AuthViewModel
import com.smartexpense.domain.usecase.AuthUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test


@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private lateinit var authUseCase: AuthUseCase
    private lateinit var viewModel: AuthViewModel
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        authUseCase = mockk()
        viewModel = AuthViewModel(authUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `email changed updates ui state`() = runTest(testDispatcher) {
        viewModel.onIntent(AuthIntent.EmailChanged("user@example.com"))

        assertEquals("user@example.com", viewModel.uiState.value.email)
    }

    @Test
    fun `toggle auth mode switches to sign up`() = runTest(testDispatcher) {
        viewModel.onIntent(AuthIntent.ToggleAuthMode)

        assertTrue(viewModel.uiState.value.isSignUp)
    }

    @Test
    fun `password mismatch shows error without signing up`() =
        runTest(testDispatcher) {
            viewModel.onIntent(AuthIntent.EmailChanged("user@example.com"))
            viewModel.onIntent(AuthIntent.PasswordChanged("Password123"))
            viewModel.onIntent(
                AuthIntent.ConfirmPasswordChanged("Different123")
            )
            viewModel.onIntent(AuthIntent.SignUp)

            assertEquals(
                "Passwords do not match",
                viewModel.uiState.value.errorMessage
            )
            assertFalse(viewModel.uiState.value.isLoading)

            coVerify(exactly = 0) {
                authUseCase.signUp(any(), any())
            }
        }

    @Test
    fun `successful sign in emits navigate to home`() =
        runTest(testDispatcher) {
            coEvery {
                authUseCase.signIn("user@example.com", "Password123")
            } returns Unit

            viewModel.effect.test {
                viewModel.onIntent(
                    AuthIntent.EmailChanged("user@example.com")
                )
                viewModel.onIntent(
                    AuthIntent.PasswordChanged("Password123")
                )
                viewModel.onIntent(AuthIntent.SignIn)

                advanceUntilIdle()

                assertEquals(
                    AuthEffect.NavigateToHome,
                    awaitItem()
                )
                assertFalse(viewModel.uiState.value.isLoading)

                cancelAndIgnoreRemainingEvents()
            }

            coVerify(exactly = 1) {
                authUseCase.signIn("user@example.com", "Password123")
            }
        }

    @Test
    fun `failed sign in updates error message`() =
        runTest(testDispatcher) {
            coEvery {
                authUseCase.signIn("user@example.com", "wrong")
            } throws IllegalArgumentException("Invalid credentials")

            viewModel.onIntent(
                AuthIntent.EmailChanged("user@example.com")
            )
            viewModel.onIntent(AuthIntent.PasswordChanged("wrong"))
            viewModel.onIntent(AuthIntent.SignIn)

            advanceUntilIdle()

            assertEquals(
                "Invalid credentials",
                viewModel.uiState.value.errorMessage
            )
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `successful sign up with active session emits navigate to home`() =
        runTest(testDispatcher) {
            coEvery {
                authUseCase.signUp("user@example.com", "Password123")
            } returns Unit
            coEvery {
                authUseCase.getCurrentUserId()
            } returns "user-123"

            viewModel.onIntent(
                AuthIntent.EmailChanged("user@example.com")
            )
            viewModel.onIntent(AuthIntent.PasswordChanged("Password123"))
            viewModel.onIntent(
                AuthIntent.ConfirmPasswordChanged("Password123")
            )

            viewModel.effect.test {
                viewModel.onIntent(AuthIntent.SignUp)

                advanceUntilIdle()

                assertEquals(
                    AuthEffect.NavigateToHome,
                    awaitItem()
                )

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `successful sign up without active session emits email confirmation`() =
        runTest(testDispatcher) {
            coEvery {
                authUseCase.signUp("user@example.com", "Password123")
            } returns Unit
            coEvery {
                authUseCase.getCurrentUserId()
            } returns null

            viewModel.onIntent(
                AuthIntent.EmailChanged("user@example.com")
            )
            viewModel.onIntent(AuthIntent.PasswordChanged("Password123"))
            viewModel.onIntent(
                AuthIntent.ConfirmPasswordChanged("Password123")
            )

            viewModel.effect.test {
                viewModel.onIntent(AuthIntent.SignUp)

                advanceUntilIdle()

                assertEquals(
                    AuthEffect.NavigateToEmailConfirmation,
                    awaitItem()
                )

                cancelAndIgnoreRemainingEvents()
            }
        }
}