package com.smartexpense.budget.state

import com.smartexpense.domain.model.BudgetInsight
import com.smartexpense.domain.model.Category

data class BudgetUiState(
    val budgetSummaries: List<BudgetSummaryState> = emptyList(),
    val insights: List<BudgetInsight> = emptyList(),
    val categories: List<Category> = emptyList(),
    val form: BudgetFormState = BudgetFormState()
)
