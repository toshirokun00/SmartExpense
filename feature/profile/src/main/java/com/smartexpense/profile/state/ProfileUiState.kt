package com.smartexpense.profile.state

data class ProfileUiState(
    val username: String = "",
    val avatarUri: String? = null,
    val email: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveMessage: String? = null
)
