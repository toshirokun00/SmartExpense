package com.smartexpense.feature.expenses.state

import com.smartexpense.domain.model.ExpenseSyncStatus
import com.smartexpense.domain.model.ExpenseType
import com.smartexpense.domain.model.PaymentMethod

data class ExpenseFormState(
    val amount: String = "",
    val description: String = "",
    val merchant: String = "",
    val categoryId: Long? = null,
    val date: Long = System.currentTimeMillis(),
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val expenseType: ExpenseType = ExpenseType.EXPENSE,
    val tags: String = "",
    val notes: String = "",
    val syncStatus: ExpenseSyncStatus = ExpenseSyncStatus.PENDING,
    val isSaving: Boolean = false
)
