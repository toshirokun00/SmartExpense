package com.portfolio.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartexpense.domain.usecase.SettingsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsUseCase: SettingsUseCase
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> =
        settingsUseCase.getSettings()
            .map { settings ->
                SettingsUiState(
                    theme = settings.theme,
                    notificationsEnabled = settings.notificationsEnabled
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
        }
    }
}