package com.smartexpense.dashoard.state

import com.smartexpense.domain.model.Expense

data class DashboardUiState(
    val totalAmount: Double = 0.0,
    val expenseCount: Int = 0,
    val recentExpenses: List<Expense> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)