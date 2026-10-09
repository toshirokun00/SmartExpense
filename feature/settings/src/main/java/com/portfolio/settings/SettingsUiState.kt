package com.portfolio.settings

import com.smartexpense.domain.model.AppTheme

data class SettingsUiState(
    val theme: AppTheme = AppTheme.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null
)