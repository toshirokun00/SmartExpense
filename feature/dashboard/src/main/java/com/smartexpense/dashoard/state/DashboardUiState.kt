package com.smartexpense.dashoard.state

import com.smartexpense.domain.model.Budget
import com.smartexpense.domain.model.Expense

data class DashboardUiState(
    val totalExpense: Double = 0.0,
    val expenseCount: Int = 0,
    val totalBudget: Double = 0.0,
    val totalBudgetSpent: Double = 0.0,
    val totalBudgetRemaining: Double = 0.0,
    val budgetProgress: Float = 0f,
    val isOverBudget: Boolean = false,
    val recentExpenses: List<Expense> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)