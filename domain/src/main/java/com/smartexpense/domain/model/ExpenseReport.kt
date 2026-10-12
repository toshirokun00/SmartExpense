package com.smartexpense.domain.model

data class ExpenseReport(
    val totalAmount: Double = 0.0,
    val expenseCount: Int = 0,
    val averageAmount: Double = 0.0,
    val categoryBreakdown: List<CategorySpending> = emptyList(),
    val dailySpending: List<DailySpending> = emptyList()
)