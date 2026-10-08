package com.portfolio.budget.state

data class BudgetScreenState(
    val showDeleteConfirmation: Boolean = false,
    val budgetToDeleteId: Long? = null
)