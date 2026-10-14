package com.smartexpense.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartexpense.auth.effect.AuthEffect
import com.smartexpense.auth.intent.AuthIntent
import com.smartexpense.auth.state.AuthUiState
import com.smartexpense.domain.usecase.AuthUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authUseCase: AuthUseCase
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
                _uiState.update {
                    it.copy(errorMessage = null, successMessage = null)
                }
            }

            AuthIntent.SignIn -> signIn()
            AuthIntent.SignUp -> signUp()
        }
    }

    private fun signIn() {
        val state = _uiState.value

        if (state.isLoading) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, successMessage = null)
            }

            try {
                authUseCase.signIn(
                    email = state.email,
                    password = state.password
                )

                _effect.emit(AuthEffect.NavigateToHome)
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = exception.message
                            ?: "Unable to sign in. Please try again."
                    )
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun signUp() {
        val state = _uiState.value

        if (state.isLoading) return

        if (state.password != state.confirmPassword) {
            _uiState.update {
                it.copy(errorMessage = "Passwords do not match")
            }
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
                    _effect.emit(AuthEffect.NavigateToHome)
                } else {
                    _effect.emit(AuthEffect.NavigateToEmailConfirmation)
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = exception.message
                            ?: "Unable to create your account. Please try again."
                    )
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}