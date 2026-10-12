package com.portfolio.reports

sealed interface ReportsIntent {

    data class DateRangeChanged(
        val startDateInclusive: Long,
        val endDateExclusive: Long
    ) : ReportsIntent

    data object Retry : ReportsIntent
}