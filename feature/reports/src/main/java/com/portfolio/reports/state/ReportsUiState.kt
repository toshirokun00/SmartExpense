package com.portfolio.reports.state

import com.smartexpense.domain.model.ExpenseReport

data class ReportsUiState(
    val report: ExpenseReport = ExpenseReport(),
    val categoryNames: Map<Long, String> = emptyMap(),
    val startDateInclusive: Long = 0L,
    val endDateExclusive: Long = 0L,
    val isStartDatePickerOpen: Boolean = false,
    val isEndDatePickerOpen: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)