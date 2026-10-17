package com.smartexpense.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartexpense.auth.effect.AuthEffect
import com.smartexpense.auth.intent.AuthIntent
import com.smartexpense.auth.screen.AuthScreen
import com.smartexpense.auth.state.AuthUiState
import com.smartexpense.auth.viewmodel.AuthViewModel
import com.smartexpense.domain.usecase.AuthUseCase
import com.smartexpense.domain.usecase.ScheduleSyncUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for [AuthScreen].
 *
 * These tests render the real composable with a fake ViewModel to keep them
 * fast and hermetic — no Koin, no real network.
 */
@RunWith(AndroidJUnit4::class)
class AuthScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var authUseCase: AuthUseCase
    private lateinit var scheduleSyncUseCase: ScheduleSyncUseCase
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        authUseCase = mockk(relaxed = true)
        scheduleSyncUseCase = mockk(relaxed = true)
        viewModel = AuthViewModel(authUseCase, scheduleSyncUseCase)
    }

    // --- initial state ---

    @Test
    fun signInTitleIsDisplayedByDefault() {
        setAuthScreen()

        composeRule.onNodeWithText("Welcome back").assertIsDisplayed()
        composeRule.onNodeWithText("Sign in to manage your expenses").assertIsDisplayed()
    }

    @Test
    fun signInButtonIsDisplayedByDefault() {
        setAuthScreen()

        composeRule.onNodeWithText("Sign in").assertIsDisplayed()
    }

    @Test
    fun toggleLinkIsDisplayedByDefault() {
        setAuthScreen()

        composeRule.onNodeWithText("Don't have an account? Sign up").assertIsDisplayed()
    }

    // --- mode toggle ---

    @Test
    fun tappingToggleSwitchesToSignUpMode() {
        setAuthScreen()

        composeRule
            .onNodeWithText("Don't have an account? Sign up")
            .performClick()

        composeRule.onNodeWithText("Create account").assertIsDisplayed()
        composeRule.onNodeWithText("Create your SmartExpense account").assertIsDisplayed()
    }

    @Test
    fun confirmPasswordFieldAppearsInSignUpMode() {
        setAuthScreen()

        composeRule
            .onNodeWithText("Don't have an account? Sign up")
            .performClick()

        composeRule.onNodeWithText("Confirm password").assertIsDisplayed()
    }

    @Test
    fun tappingToggleInSignUpModeSwitchesBackToSignIn() {
        setAuthScreen()

        composeRule
            .onNodeWithText("Don't have an account? Sign up")
            .performClick()

        composeRule
            .onNodeWithText("Already have an account? Sign in")
            .performClick()

        composeRule.onNodeWithText("Welcome back").assertIsDisplayed()
    }

    // --- sign in flow ---

    @Test
    fun enteringEmailAndPasswordAndTappingSignInCallsUseCase() {
        coEvery { authUseCase.signIn(any(), any()) } returns Unit

        setAuthScreen()

        composeRule.onNodeWithText("Email").performTextInput("user@example.com")
        composeRule.onNodeWithText("Password").performTextInput("Password123")
        composeRule.onNodeWithText("Sign in").performClick()

        composeRule.waitForIdle()

        coVerify { authUseCase.signIn("user@example.com", "Password123") }
    }

    @Test
    fun errorMessageIsDisplayedWhenSignInFails() {
        coEvery {
            authUseCase.signIn(any(), any())
        } throws IllegalArgumentException("Invalid credentials")

        setAuthScreen()

        composeRule.onNodeWithText("Email").performTextInput("user@example.com")
        composeRule.onNodeWithText("Password").performTextInput("wrongpassword")
        composeRule.onNodeWithText("Sign in").performClick()

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Invalid credentials").assertIsDisplayed()
    }

    @Test
    fun signInButtonIsDisabledWhileLoading() {
        // Block sign-in indefinitely so the loading state persists.
        coEvery { authUseCase.signIn(any(), any()) } coAnswers {
            kotlinx.coroutines.delay(30_000)
        }

        setAuthScreen()

        composeRule.onNodeWithText("Email").performTextInput("user@example.com")
        composeRule.onNodeWithText("Password").performTextInput("Password123")
        composeRule.onNodeWithText("Sign in").performClick()

        // While the coroutine is suspended the button must be disabled.
        composeRule.onNodeWithText("Sign in").assertIsNotEnabled()
    }

    // --- sign up flow ---

    @Test
    fun passwordMismatchShowsErrorWithoutCallingUseCase() {
        setAuthScreen()

        composeRule
            .onNodeWithText("Don't have an account? Sign up")
            .performClick()

        composeRule.onNodeWithText("Email").performTextInput("user@example.com")
        composeRule.onNodeWithText("Password").performTextInput("Password123")
        composeRule.onNodeWithText("Confirm password").performTextInput("Different123")
        composeRule.onNodeWithText("Create account").performClick()

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Passwords do not match").assertIsDisplayed()
        coVerify(exactly = 0) { authUseCase.signUp(any(), any()) }
    }

    @Test
    fun successfulSignUpNavigatesToHome() {
        var navigatedToHome = false

        coEvery { authUseCase.signUp(any(), any()) } returns Unit
        coEvery { authUseCase.getCurrentUserId() } returns "user-123"

        setAuthScreen(onNavigateToHome = { navigatedToHome = true })

        composeRule
            .onNodeWithText("Don't have an account? Sign up")
            .performClick()

        composeRule.onNodeWithText("Email").performTextInput("user@example.com")
        composeRule.onNodeWithText("Password").performTextInput("Password123")
        composeRule.onNodeWithText("Confirm password").performTextInput("Password123")
        composeRule.onNodeWithText("Create account").performClick()

        composeRule.waitForIdle()

        assert(navigatedToHome) { "Expected navigation to Home but it did not occur" }
    }

    @Test
    fun successfulSignUpWithoutSessionNavigatesToEmailConfirmation() {
        var navigatedToEmailConfirmation = false

        coEvery { authUseCase.signUp(any(), any()) } returns Unit
        coEvery { authUseCase.getCurrentUserId() } returns null

        setAuthScreen(
            onNavigateToEmailConfirmation = { navigatedToEmailConfirmation = true }
        )

        composeRule
            .onNodeWithText("Don't have an account? Sign up")
            .performClick()

        composeRule.onNodeWithText("Email").performTextInput("user@example.com")
        composeRule.onNodeWithText("Password").performTextInput("Password123")
        composeRule.onNodeWithText("Confirm password").performTextInput("Password123")
        composeRule.onNodeWithText("Create account").performClick()

        composeRule.waitForIdle()

        assert(navigatedToEmailConfirmation) {
            "Expected navigation to EmailConfirmation but it did not occur"
        }
    }

    // --- helpers ---

    private fun setAuthScreen(
        onNavigateToHome: () -> Unit = {},
        onNavigateToEmailConfirmation: () -> Unit = {}
    ) {
        composeRule.setContent {
            AuthScreen(
                viewModel = viewModel,
                onNavigateToHome = onNavigateToHome,
                onNavigateToEmailConfirmation = onNavigateToEmailConfirmation
            )
        }
    }
}
