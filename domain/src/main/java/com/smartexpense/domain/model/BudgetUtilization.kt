package com.smartexpense.domain.model

/**
 * The computed utilization of a single budget for its own period.
 *
 *  - [remainingAmount]  = budget limit - amount spent.
 *  - [utilizationPercentage] = spent / limit * 100 (uncapped, so values above 100
 *    are preserved for exceeded budgets, and 0.0 for a non-positive limit).
 *  - [progress] = [utilizationPercentage] clamped to 0..1 for visual progress bars.
 *  - [status] derives from [utilizationPercentage] and the configured warning threshold.
 */
data class BudgetUtilization(
    val budget: Budget,
    val spentAmount: Double,
    val remainingAmount: Double,
    val utilizationPercentage: Double,
    val progress: Float,
    val status: BudgetStatus
) {
    val isOverBudget: Boolean
        get() = status == BudgetStatus.EXCEEDED

    val overBudgetAmount: Double
        get() = if (spentAmount > budget.amount) spentAmount - budget.amount else 0.0
}
