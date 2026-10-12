package com.portfolio.reports

import com.smartexpense.domain.model.ExpenseReport

data class ReportsUiState(
    val report: ExpenseReport = ExpenseReport(),
    val startDateInclusive: Long = 0L,
    val endDateExclusive: Long = 0L,
    val isLoading: Boolean = true,
    val error: String? = null
)