package com.portfolio.reports

sealed interface ReportsEffect {
    data class ShowError(val message: String) : ReportsEffect
}