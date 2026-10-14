package com.portfolio.smartexpense

import com.portfolio.smartexpense.navigation.Route

data class StartupUiState(
    val isLoading: Boolean = true,
    val startDestination: Route? = null
)