package com.portfolio.settings.effect

sealed interface SettingsEffect {
    data object NavigateToSignIn : SettingsEffect
    data class ShowError(val message: String) : SettingsEffect
}