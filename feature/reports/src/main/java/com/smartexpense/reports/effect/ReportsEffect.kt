package com.smartexpense.reports.effect

sealed interface ReportsEffect {
    data class ShowError(val message: String) : ReportsEffect
}