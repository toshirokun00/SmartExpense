package com.smartexpense

import com.smartexpense.navigation.Route

data class StartupUiState(
    val isLoading: Boolean = true,
    val startDestination: Route? = null
)