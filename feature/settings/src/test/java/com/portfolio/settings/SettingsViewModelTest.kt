package com.portfolio.settings

import app.cash.turbine.test
import com.smartexpense.domain.model.AppSettings
import com.smartexpense.domain.model.AppTheme
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
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        settingsUseCase = mockk()

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

        viewModel = SettingsViewModel(settingsUseCase)
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
    fun settingsFlowHandlesRepositoryError() = runTest {
        every {
            settingsUseCase.getSettings()
        } returns flow {
            throw IllegalStateException("Unable to load settings")
        }

        viewModel = SettingsViewModel(settingsUseCase)

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

}