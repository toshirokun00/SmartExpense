package com.smartexpense.auth.effect

sealed interface AuthEffect {
    data object NavigateToHome : AuthEffect
    data object NavigateToLogin : AuthEffect
    data object NavigateToEmailConfirmation : AuthEffect
}