package com.smartexpense.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartexpense.domain.model.UserProfile
import com.smartexpense.domain.usecase.AuthUseCase
import com.smartexpense.domain.usecase.ProfileUseCase
import com.smartexpense.profile.intent.ProfileIntent
import com.smartexpense.profile.state.ProfileUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

class ProfileViewModel(
    private val profileUseCase: ProfileUseCase,
    private val authUseCase: AuthUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState

    private val _effect = MutableSharedFlow<ProfileEffect>()
    val effect = _effect.asSharedFlow()

    init {
        loadInitialProfile()
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.UsernameChanged -> {
                _uiState.value = _uiState.value.copy(username = intent.username, error = null)
                viewModelScope.launch { saveProfile() }
            }

            is ProfileIntent.AvatarChanged -> {
                _uiState.value = _uiState.value.copy(avatarUri = intent.avatarUri, error = null)
                viewModelScope.launch { saveProfile() }
            }

            is ProfileIntent.SaveProfile -> {
                viewModelScope.launch { saveProfile() }
            }

            is ProfileIntent.SignOut -> {
                viewModelScope.launch {
                    try {
                        authUseCase.signOut()
                        _effect.emit(ProfileEffect.NavigateToSignIn)
                    } catch (exception: Exception) {
                        _effect.emit(
                            ProfileEffect.ShowError(
                                exception.message ?: "Failed to sign out"
                            )
                        )
                    }
                }
            }
        }
    }

    private suspend fun saveProfile() {
        val current = _uiState.value
        _uiState.value = current.copy(isSaving = true, error = null, saveMessage = null)
        try {
            profileUseCase.saveProfile(
                username = current.username,
                avatarUri = current.avatarUri
            )
            _uiState.value = _uiState.value.copy(saveMessage = "Profile saved")
            delay(1500)
            _uiState.value = _uiState.value.copy(saveMessage = null)
        } catch (exception: Exception) {
            _uiState.value = _uiState.value.copy(
                error = exception.message ?: "Failed to save profile"
            )
        } finally {
            _uiState.value = _uiState.value.copy(isSaving = false)
        }
    }

    private fun loadInitialProfile() {
        viewModelScope.launch {
            try {
                // Hydrate local Room with the remote profile first so a fresh
                // install picks up the user's previously-set nickname. Bound
                // it with a timeout so a slow/unreachable network can't leave
                // the screen stuck on the loading state.
                try {
                    withTimeout(HYDRATE_TIMEOUT_MS) {
                        profileUseCase.hydrateLocalFromRemote()
                    }
                } catch (_: Exception) {
                    // Remote unavailable — fall back to local data below.
                }

                val details = profileUseCase.getCurrentUserDetails()
                val profile = profileUseCase.observeLocalProfileForCurrentUser()
                _uiState.value = ProfileUiState(
                    username = profile?.username.orEmpty(),
                    avatarUri = profile?.avatarUri,
                    email = details?.email.orEmpty(),
                    isLoading = false
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = exception.message ?: "Failed to load profile"
                )
            }
        }
    }

    private companion object {
        const val HYDRATE_TIMEOUT_MS = 5_000L
    }
}
