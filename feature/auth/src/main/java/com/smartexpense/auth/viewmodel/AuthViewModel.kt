package com.smartexpense.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartexpense.auth.effect.AuthEffect
import com.smartexpense.auth.intent.AuthIntent
import com.smartexpense.auth.state.AuthUiState
import com.smartexpense.domain.network.AlwaysConnectedNetworkStatus
import com.smartexpense.domain.network.NetworkStatusProvider
import com.smartexpense.domain.network.NO_INTERNET_MESSAGE
import com.smartexpense.domain.usecase.AuthUseCase
import com.smartexpense.domain.usecase.ScheduleSyncUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authUseCase: AuthUseCase,
    private val scheduleSyncUseCase: ScheduleSyncUseCase,
    private val networkStatus: NetworkStatusProvider = AlwaysConnectedNetworkStatus
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<AuthEffect>()
    val effect = _effect.asSharedFlow()

    fun onIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.EmailChanged -> {
                _uiState.update {
                    it.copy(email = intent.email, errorMessage = null)
                }
            }

            is AuthIntent.PasswordChanged -> {
                _uiState.update {
                    it.copy(password = intent.password, errorMessage = null)
                }
            }

            is AuthIntent.ConfirmPasswordChanged -> {
                _uiState.update {
                    it.copy(
                        confirmPassword = intent.password,
                        errorMessage = null
                    )
                }
            }

            AuthIntent.ToggleAuthMode -> {
                _uiState.update {
                    it.copy(
                        isSignUp = !it.isSignUp,
                        errorMessage = null,
                        successMessage = null
                    )
                }
            }

            AuthIntent.TogglePasswordVisibility -> {
                _uiState.update {
                    it.copy(isPasswordVisible = !it.isPasswordVisible)
                }
            }

            AuthIntent.ClearMessage -> {
                clearMessage()
            }

            AuthIntent.SignIn -> signIn()
            AuthIntent.SignUp -> signUp()
        }
    }

    /**
     * Every shown message gets a new event id, even when the wording is
     * identical to the previous one. The UI keys the toast on that id so a
     * repeated tap always produces a fresh toast.
     */
    private fun showMessage(error: String? = null, success: String? = null) {
        _uiState.update {
            it.copy(
                errorMessage = error,
                successMessage = success,
                messageEventId = it.messageEventId + 1
            )
        }
    }

    private fun clearMessage() {
        _uiState.update {
            it.copy(errorMessage = null, successMessage = null)
        }
    }

    /**
     * Supabase exceptions can contain full HTTP requests, including headers
     * and keys. Keep short, human-friendly messages as-is and translate the
     * raw transport errors into something safe to show in a toast.
     */
    private fun friendlyError(exception: Exception, fallback: String): String {
        if (isNetworkFailure(exception)) return NO_INTERNET_MESSAGE

        val raw = exception.message?.trim().orEmpty()
        if (raw.isEmpty()) return fallback

        val lower = raw.lowercase()
        val looksLikeTransportError = raw.length > 120 ||
            "http" in lower ||
            "headers:" in lower ||
            "url:" in lower
        if (!looksLikeTransportError) return raw

        return when {
            "email_not_confirmed" in lower ->
                "Please confirm your email before signing in."
            "invalid_credentials" in lower ->
                "Invalid email or password. Please try again."
            "user_already_registered" in lower ->
                "An account with this email already exists."
            "weak_password" in lower ->
                "Please choose a stronger password."
            "rate_limit" in lower || "429" in raw ->
                "Too many attempts. Please try again shortly."
            else -> fallback
        }
    }

    private fun isNetworkFailure(exception: Exception): Boolean {
        if (!networkStatus.isConnected()) return true

        val exceptionName = exception::class.java.name.lowercase()
        val raw = exception.message?.lowercase().orEmpty()
        return listOf(
            "unknownhost",
            "socket",
            "connectionreset",
            "connecttimeout",
            "sslhandshake"
        ).any { it in exceptionName } || listOf(
            "unable to resolve host",
            "failed to connect",
            "network is unreachable",
            "connection timed out",
            "connection reset",
            "no internet",
            "software caused connection abort"
        ).any { it in raw }
    }

    private fun signIn() {
        val state = _uiState.value

        if (state.isLoading) return

        if (!networkStatus.isConnected()) {
            showMessage(error = NO_INTERNET_MESSAGE)
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, successMessage = null)
            }

            try {
                authUseCase.signIn(
                    email = state.email,
                    password = state.password
                )

                scheduleSyncUseCase()

                _effect.emit(AuthEffect.NavigateToHome)
            } catch (exception: Exception) {
                showMessage(
                    error = friendlyError(
                        exception,
                        "Unable to sign in. Please try again."
                    )
                )
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun signUp() {
        val state = _uiState.value

        if (state.isLoading) return

        if (state.password != state.confirmPassword) {
            showMessage(error = "Passwords do not match")
            return
        }

        if (!networkStatus.isConnected()) {
            showMessage(error = NO_INTERNET_MESSAGE)
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, successMessage = null)
            }

            try {
                authUseCase.signUp(
                    email = state.email,
                    password = state.password
                )

                val currentUserId = authUseCase.getCurrentUserId()

                if (currentUserId != null) {
                    scheduleSyncUseCase()
                    _effect.emit(AuthEffect.NavigateToHome)
                } else {
                    _effect.emit(AuthEffect.NavigateToEmailConfirmation)
                }
            } catch (exception: Exception) {
                showMessage(
                    error = friendlyError(
                        exception,
                        "Unable to create your account. Please try again."
                    )
                )
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
