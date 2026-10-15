package com.smartexpense.budget.state

data class BudgetScreenState(
    val showDeleteConfirmation: Boolean = false,
    val budgetToDeleteId: Long? = null
)