package com.smartexpense.auth.intent

sealed interface AuthIntent {
    data class EmailChanged(val email: String) : AuthIntent
    data class PasswordChanged(val password: String) : AuthIntent
    data class ConfirmPasswordChanged(val password: String) : AuthIntent

    data object SignIn : AuthIntent
    data object SignUp : AuthIntent
    data object ToggleAuthMode : AuthIntent
    data object TogglePasswordVisibility : AuthIntent
    data object ClearMessage : AuthIntent
}