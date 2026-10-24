package com.smartexpense.recurring.state

import com.smartexpense.domain.model.Category
import com.smartexpense.domain.model.RecurrenceFrequency
import java.util.UUID

data class RecurringExpenseFormState(
    val editingScheduleId: Long? = null,
    val clientId: String = UUID.randomUUID().toString(),
    val description: String = "",
    val amount: String = "",
    val categoryId: Long? = null,
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val interval: String = "1",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false
)

data class RecurringExpenseFormUiState(
    val form: RecurringExpenseFormState = RecurringExpenseFormState(),
    val categories: List<Category> = emptyList()
)
