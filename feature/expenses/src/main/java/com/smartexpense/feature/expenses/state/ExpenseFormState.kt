package com.smartexpense.feature.expenses.state

import com.smartexpense.domain.model.ExpenseSyncStatus

data class ExpenseFormState(
    val amount: String = "",
    val description: String = "",
    val merchant: String = "",
    val categoryId: Long? = null,
    val date: Long = System.currentTimeMillis(),
    val syncStatus: ExpenseSyncStatus = ExpenseSyncStatus.PENDING,
    val isSaving: Boolean = false
)
