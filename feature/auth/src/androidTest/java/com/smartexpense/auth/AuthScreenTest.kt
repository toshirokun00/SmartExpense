package com.smartexpense.auth

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartexpense.auth.intent.AuthIntent
import com.smartexpense.auth.screen.AuthScreen
import com.smartexpense.auth.viewmodel.AuthViewModel
import com.smartexpense.domain.usecase.AuthUseCase
import com.smartexpense.domain.usecase.ScheduleSyncUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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

        composeRule.onNodeWithText("Spendora").assertIsDisplayed()
        composeRule.onNodeWithText("Sign in to manage your expenses").assertIsDisplayed()
    }

    @Test
    fun signInButtonIsDisplayedByDefault() {
        setAuthScreen()

        submitButton().assertIsDisplayed()
    }

    @Test
    fun toggleLinkIsDisplayedByDefault() {
        setAuthScreen()

        composeRule
            .onNodeWithText("Don't have an account?", substring = true)
            .assertIsDisplayed()
    }

    // --- mode toggle ---

    @Test
    fun tappingToggleSwitchesToSignUpMode() {
        setAuthScreen()

        composeRule
            .onNodeWithText("Don't have an account?", substring = true)
            .performClick()

        submitButton().assertIsDisplayed()
        composeRule
            .onNodeWithText("Create your account and start tracking")
            .assertIsDisplayed()
    }

    @Test
    fun confirmPasswordFieldAppearsInSignUpMode() {
        setAuthScreen()

        composeRule
            .onNodeWithText("Don't have an account?", substring = true)
            .performClick()

        field("Re-enter your password").assertIsDisplayed()
    }

    @Test
    fun tappingToggleInSignUpModeSwitchesBackToSignIn() {
        setAuthScreen()

        composeRule
            .onNodeWithText("Don't have an account?", substring = true)
            .performClick()

        composeRule
            .onNodeWithText("Already have an account?", substring = true)
            .performClick()

        composeRule.onNodeWithText("Spendora").assertIsDisplayed()
    }

    // --- sign in flow ---

    @Test
    fun enteringEmailAndPasswordAndTappingSignInCallsUseCase() {
        coEvery { authUseCase.signIn(any(), any()) } returns Unit

        setAuthScreen()

        field("Enter your email").performTextInput("user@example.com")
        field("Enter your password").performTextInput("Password123")
        submitButton().performClick()

        composeRule.waitForIdle()

        coVerify { authUseCase.signIn("user@example.com", "Password123") }
    }

    @Test
    fun errorMessageIsDisplayedWhenSignInFails() {
        coEvery {
            authUseCase.signIn(any(), any())
        } throws IllegalArgumentException("Invalid credentials")

        setAuthScreen()

        field("Enter your email").performTextInput("user@example.com")
        field("Enter your password").performTextInput("wrongpassword")
        submitButton().performClick()

        awaitErrorMessage()
    }

    @Test
    fun signInButtonIsDisabledWhileLoading() {
        // Block sign-in indefinitely so the loading state persists.
        coEvery { authUseCase.signIn(any(), any()) } coAnswers {
            kotlinx.coroutines.delay(30_000)
        }

        setAuthScreen()

        field("Enter your email").performTextInput("user@example.com")
        field("Enter your password").performTextInput("Password123")
        submitButton().performClick()

        // While the coroutine is suspended the button must be disabled.
        submitButton().assertIsNotEnabled()
    }

    // --- sign up flow ---

    @Test
    fun passwordMismatchShowsErrorWithoutCallingUseCase() {
        setAuthScreen()

        composeRule
            .onNodeWithText("Don't have an account?", substring = true)
            .performClick()

        field("Enter your email").performTextInput("user@example.com")
        field("Enter your password").performTextInput("Password123")
        field("Re-enter your password").performTextInput("Different123")
        submitButton().performClick()

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
            .onNodeWithText("Don't have an account?", substring = true)
            .performClick()

        field("Enter your email").performTextInput("user@example.com")
        field("Enter your password").performTextInput("Password123")
        field("Re-enter your password").performTextInput("Password123")
        submitButton().performClick()

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
            .onNodeWithText("Don't have an account?", substring = true)
            .performClick()

        field("Enter your email").performTextInput("user@example.com")
        field("Enter your password").performTextInput("Password123")
        field("Re-enter your password").performTextInput("Password123")
        submitButton().performClick()

        composeRule.waitForIdle()

        assert(navigatedToEmailConfirmation) {
            "Expected navigation to EmailConfirmation but it did not occur"
        }
    }

    @Test
    fun sameErrorShowsToastAgainOnNextAttempt() {
        coEvery {
            authUseCase.signIn(any(), any())
        } throws IllegalArgumentException("Invalid credentials")

        setAuthScreen()

        field("Enter your email").performTextInput("user@example.com")
        field("Enter your password").performTextInput("Password123")
        submitButton().performClick()
        awaitErrorMessage()

        // Dismiss the first toast, then repeat the same failing action.
        composeRule
            .onNodeWithContentDescription("Dismiss message")
            .performClick()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule
                .onAllNodesWithText("Invalid credentials")
                .fetchSemanticsNodes()
                .isEmpty()
        }

        submitButton().performClick()

        awaitErrorMessage()
    }

    private fun awaitErrorMessage() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithText("Invalid credentials")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText("Invalid credentials").assertIsDisplayed()
    }

    // --- helpers ---

    private fun hasRole(role: Role): SemanticsMatcher =
        SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    /** Matches an outlined text field by its placeholder, not its (hidden) label. */
    private fun field(placeholder: String) =
        composeRule.onNode(hasText(placeholder) and hasSetTextAction())

    /** Matches the submit Button, so the segmented Sign in / Sign up tabs don't collide. */
    private fun submitButton() =
        composeRule.onNode(hasTestTag("authSubmitButton") and hasRole(Role.Button))

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
