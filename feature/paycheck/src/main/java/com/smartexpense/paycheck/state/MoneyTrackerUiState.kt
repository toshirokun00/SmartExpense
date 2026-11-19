package com.smartexpense.paycheck.state

import com.smartexpense.domain.model.PaycheckEnvelope

data class MoneyTrackerUiState(
    val envelopes: List<PaycheckEnvelope> = emptyList(),
    val isLoading: Boolean = true
) {
    val totalIncome: Double get() = envelopes.sumOf { it.income }

    val totalBills: Double get() = envelopes.sumOf { it.billsTotal }

    val totalExpenses: Double get() = envelopes.sumOf { it.expensesTotal }

    val totalDeductions: Double get() = totalBills + totalExpenses

    val totalRemaining: Double get() = totalIncome - totalDeductions

    val isShort: Boolean get() = totalRemaining < 0

    val usedFraction: Float
        get() = when {
            totalIncome > 0 -> (totalDeductions / totalIncome)
                .toFloat()
                .coerceIn(0f, 1f)
            totalDeductions > 0 -> 1f
            else -> 0f
        }
}
