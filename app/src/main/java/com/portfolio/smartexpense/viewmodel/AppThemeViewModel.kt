package com.portfolio.smartexpense.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartexpense.domain.model.AppTheme
import com.smartexpense.domain.usecase.SettingsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class AppThemeViewModel(
    settingsUseCase: SettingsUseCase
) : ViewModel() {

    val theme: StateFlow<AppTheme> =
        settingsUseCase.getSettings()
            .map { settings -> settings.theme }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Companion.WhileSubscribed(5_000),
                initialValue = AppTheme.SYSTEM
            )
}