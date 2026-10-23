package com.smartexpense.domain.model

/**
 * Indicates how a budget is tracking against its configured warning threshold.
 *
 * [utilizationPercentage] is preserved above 100% so that [EXCEEDED] budgets
 * still expose how far over the limit they are.
 */
enum class BudgetStatus {
    WITHIN_BUDGET,
    APPROACHING_LIMIT,
    EXCEEDED
}
