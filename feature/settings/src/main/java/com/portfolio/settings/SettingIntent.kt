package com.portfolio.settings

import com.smartexpense.domain.model.AppTheme

sealed interface SettingsIntent {

    data class ThemeChanged(
        val theme: AppTheme
    ) : SettingsIntent

    data class NotificationsChanged(
        val enabled: Boolean
    ) : SettingsIntent
}