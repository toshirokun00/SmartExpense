package com.portfolio.budget.state

data class BudgetFormState(
    val categoryId: Long? = null,
    val amount: String = "",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val editingBudgetId: Long? = null
)