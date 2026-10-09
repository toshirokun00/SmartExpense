package com.smartexpense.dashoard.effect

sealed interface DashboardEffect {
    data class ShowError(val message: String) : DashboardEffect
}