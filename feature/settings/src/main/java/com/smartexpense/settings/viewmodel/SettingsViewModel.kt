package com.smartexpense.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartexpense.settings.effect.SettingsEffect
import com.smartexpense.settings.state.SettingsUiState
import com.smartexpense.settings.intent.SettingsIntent
import com.smartexpense.domain.usecase.AuthUseCase
import com.smartexpense.domain.usecase.SettingsUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class SettingsViewModel(
    private val settingsUseCase: SettingsUseCase,
    private val authUseCase: AuthUseCase
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> =
        settingsUseCase.getSettings()
            .map { settings ->
                SettingsUiState(
                    theme = settings.theme,
                    notificationsEnabled = settings.notificationsEnabled,
                    budgetAlertThresholds = settings.budgetAlertThresholds
                )
            }
            .catch { exception ->
                emit(
                    SettingsUiState(
                        error = exception.message
                            ?: "Failed to load settings"
                    )
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SettingsUiState(
                    isLoading = true
                )
            )

    private val _effect = MutableSharedFlow<SettingsEffect>()
    val effect = _effect.asSharedFlow()

    fun onIntent(intent: SettingsIntent) {
        when (intent) {

            is SettingsIntent.ThemeChanged -> {
                viewModelScope.launch {
                    settingsUseCase.setTheme(intent.theme)
                }
            }

            is SettingsIntent.NotificationsChanged -> {
                viewModelScope.launch {
                    settingsUseCase.setNotificationsEnabled(
                        intent.enabled
                    )
                }
            }
            is SettingsIntent.BudgetAlertThresholdsChanged -> {
                viewModelScope.launch {
                    settingsUseCase.setBudgetAlertThresholds(
                        intent.thresholds
                    )
                }
            }
            is SettingsIntent.SignOut -> {
                viewModelScope.launch {
                    try {
                        authUseCase.signOut()
                        _effect.emit(SettingsEffect.NavigateToSignIn)
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        _effect.emit(
                            SettingsEffect.ShowError(
                                exception.message ?: "Failed to sign out"
                            )
                        )
                    }
                }
            }
        }
    }
}