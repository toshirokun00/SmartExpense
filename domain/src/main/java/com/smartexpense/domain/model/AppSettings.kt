package com.smartexpense.domain.model

data class AppSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val notificationsEnabled: Boolean = true
)
