package com.smartexpense.profile.viewmodel

sealed interface ProfileEffect {
    data object NavigateToSettings : ProfileEffect
    data object NavigateToSignIn : ProfileEffect
    data class ShowError(val message: String) : ProfileEffect
}
