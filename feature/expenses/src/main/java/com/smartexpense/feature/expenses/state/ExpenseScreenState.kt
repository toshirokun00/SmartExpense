package com.smartexpense.feature.expenses.state

data class ExpenseScreenState(
    val showDeleteConfirmation: Boolean = false,
    val expenseToDeleteId: Long? = null
)
