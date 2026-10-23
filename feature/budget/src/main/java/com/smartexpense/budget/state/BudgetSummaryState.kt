package com.smartexpense.budget.state

import com.smartexpense.domain.model.Budget
import com.smartexpense.domain.model.BudgetStatus

data class BudgetSummaryState(
    val budget: Budget,
    val categoryName: String,
    val spentAmount: Double,
    val remainingAmount: Double,
    val progress: Float,
    val isOverBudget: Boolean,
    val utilizationPercentage: Double = 0.0,
    val status: BudgetStatus = BudgetStatus.WITHIN_BUDGET
)
