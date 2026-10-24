package com.smartexpense.recurring.state

import com.smartexpense.domain.model.RecurringExpenseSchedule
import com.smartexpense.domain.model.RecurringScheduleStatus

data class RecurringExpenseSummaryState(
    val schedule: RecurringExpenseSchedule,
    val categoryName: String,
    val frequencyLabel: String,
    val status: RecurringScheduleStatus
)

data class RecurringExpenseUiState(
    val isLoading: Boolean = true,
    val schedules: List<RecurringExpenseSummaryState> = emptyList(),
    val errorMessage: String? = null
)
