package com.portfolio.budget.state

import com.smartexpense.domain.model.Budget

data class BudgetSummaryState(
    val budget: Budget,
    val categoryName: String,
    val spentAmount: Double,
    val remainingAmount: Double,
    val progress: Float,
    val isOverBudget: Boolean
)