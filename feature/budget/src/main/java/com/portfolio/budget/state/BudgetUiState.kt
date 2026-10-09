package com.portfolio.budget.state

import com.smartexpense.domain.model.Budget
import com.smartexpense.domain.model.Category

data class BudgetUiState(
    val budgets: List<Budget> = emptyList(),
    val budgetSummaries: List<BudgetSummaryState> = emptyList(),
    val categories: List<Category> = emptyList(),
    val form: BudgetFormState = BudgetFormState(),
    val isLoading: Boolean = false,
    val error: String? = null
)