package com.portfolio.reports.intent

sealed interface ReportsIntent {
    data class DateRangeChanged(
        val startDateInclusive: Long,
        val endDateExclusive: Long
    ) : ReportsIntent

    data object StartDatePickerOpened : ReportsIntent
    data object EndDatePickerOpened : ReportsIntent
    data object DatePickerDismissed : ReportsIntent

    data object Retry : ReportsIntent
}