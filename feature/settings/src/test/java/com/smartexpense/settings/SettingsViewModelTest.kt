package com.smartexpense.settings

import app.cash.turbine.test
import com.smartexpense.settings.effect.SettingsEffect
import com.smartexpense.settings.intent.SettingsIntent
import com.smartexpense.settings.viewmodel.SettingsViewModel
import com.smartexpense.domain.model.AppSettings
import com.smartexpense.domain.model.AppTheme
import com.smartexpense.domain.usecase.AuthUseCase
import com.smartexpense.domain.usecase.SettingsUseCase
import com.smartexpense.test.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var settingsUseCase: SettingsUseCase
    private lateinit var authUseCase: AuthUseCase
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        settingsUseCase = mockk()
        authUseCase = mockk()

        every {
            settingsUseCase.getSettings()
        } returns flowOf(
            AppSettings(
                theme = AppTheme.SYSTEM,
                notificationsEnabled = true
            )
        )

        coEvery {
            settingsUseCase.setTheme(any())
        } returns Unit

        coEvery {
            settingsUseCase.setNotificationsEnabled(any())
        } returns Unit

        coEvery {
            settingsUseCase.setBudgetAlertThresholds(any())
        } returns Unit

        coEvery {
            authUseCase.signOut()
        } returns Unit


        viewModel = SettingsViewModel(settingsUseCase, authUseCase)
    }


    @Test
    fun settingsFlowEmitsLoadedSettings() = runTest {
        viewModel.uiState.test {
            // First emission: initial loading state
            val initialState = awaitItem()
            assertTrue(initialState.isLoading)

            // Second emission: loaded settings
            val state = awaitItem()

            assertEquals(AppTheme.SYSTEM, state.theme)
            assertTrue(state.notificationsEnabled)
            assertEquals(setOf(50, 80, 90, 100), state.budgetAlertThresholds)
            assertFalse(state.isLoading)
            assertEquals(null, state.error)

            cancelAndIgnoreRemainingEvents()
        }
    }


    @Test
    fun themeChangedCallsUseCase() = runTest {
        viewModel.onIntent(
            SettingsIntent.ThemeChanged(AppTheme.DARK)
        )

        advanceUntilIdle()

        coVerify(exactly = 1) {
            settingsUseCase.setTheme(AppTheme.DARK)
        }
    }

    @Test
    fun notificationsChangedCallsUseCase() = runTest {
        viewModel.onIntent(
            SettingsIntent.NotificationsChanged(false)
        )

        advanceUntilIdle()

        coVerify(exactly = 1) {
            settingsUseCase.setNotificationsEnabled(false)
        }
    }

    @Test
    fun budgetAlertThresholdsChangedCallsUseCase() = runTest {
        viewModel.onIntent(
            SettingsIntent.BudgetAlertThresholdsChanged(
                setOf(50, 80, 100)
            )
        )

        advanceUntilIdle()

        coVerify(exactly = 1) {
            settingsUseCase.setBudgetAlertThresholds(setOf(50, 80, 100))
        }
    }

    @Test
    fun budgetAlertThresholdsCanBeClearedCompletely() = runTest {
        viewModel.onIntent(
            SettingsIntent.BudgetAlertThresholdsChanged(emptySet())
        )

        advanceUntilIdle()

        coVerify(exactly = 1) {
            settingsUseCase.setBudgetAlertThresholds(emptySet())
        }
    }

    @Test
    fun settingsFlowEmitsConfiguredBudgetAlertThresholds() = runTest {
        every {
            settingsUseCase.getSettings()
        } returns flowOf(
            AppSettings(
                theme = AppTheme.SYSTEM,
                notificationsEnabled = true,
                budgetAlertThresholds = setOf(50, 90)
            )
        )

        viewModel = SettingsViewModel(settingsUseCase, authUseCase)

        viewModel.uiState.test {
            awaitItem()
            val state = awaitItem()

            assertEquals(setOf(50, 90), state.budgetAlertThresholds)

            cancelAndIgnoreRemainingEvents()
        }
    }


    @Test
    fun settingsFlowHandlesRepositoryError() = runTest {
        every {
            settingsUseCase.getSettings()
        } returns flow {
            throw IllegalStateException("Unable to load settings")
        }

        viewModel = SettingsViewModel(settingsUseCase, authUseCase)

        viewModel.uiState.test {
            // Initial state: isLoading = true
            val initialState = awaitItem()
            assertTrue(initialState.isLoading)

            // Wait for the flow error to be handled
            val errorState = awaitItem()

            assertEquals(
                "Unable to load settings",
                errorState.error
            )
            assertFalse(errorState.isLoading)

            cancelAndIgnoreRemainingEvents()
        }
    }
    @Test
    fun signOutSuccessEmitsNavigateToSignIn() = runTest {
        viewModel.effect.test {
            viewModel.onIntent(SettingsIntent.SignOut)

            assertEquals(
                SettingsEffect.NavigateToSignIn,
                awaitItem()
            )

            coVerify(exactly = 1) {
                authUseCase.signOut()
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun signOutFailureEmitsShowError() = runTest {
        coEvery {
            authUseCase.signOut()
        } throws IllegalStateException("Unable to sign out")

        viewModel.effect.test {
            viewModel.onIntent(SettingsIntent.SignOut)

            assertEquals(
                SettingsEffect.ShowError("Unable to sign out"),
                awaitItem()
            )

            coVerify(exactly = 1) {
                authUseCase.signOut()
            }

            cancelAndIgnoreRemainingEvents()
        }
    }


}