package com.smartexpense.domain.model

/**
 * A deterministic, locally-computed spending insight.
 *
 * Insights carry structured values (amounts, percentages) rather than pre-formatted
 * strings so the presentation layer can apply the app's currency/date conventions.
 */
sealed interface BudgetInsight {

    /** A budget that is nearing its configured warning threshold. */
    data class ApproachingLimit(
        val budgetId: Long,
        val categoryName: String,
        val utilizationPercentage: Double,
        val remainingAmount: Double
    ) : BudgetInsight

    /** A budget whose spending exceeds its limit. */
    data class ExceededLimit(
        val budgetId: Long,
        val categoryName: String,
        val overAmount: Double,
        val utilizationPercentage: Double
    ) : BudgetInsight

    /**
     * Spending for the focused period compared with the equivalent elapsed portion
     * of the previous period of the same length.
     */
    data class SpendingTrend(
        val currentAmount: Double,
        val previousAmount: Double,
        val changePercentage: Double,
        val isIncrease: Boolean
    ) : BudgetInsight

    /** A category whose spending increased notably versus the previous comparable period. */
    data class CategorySpendingChange(
        val categoryId: Long,
        val categoryName: String,
        val currentAmount: Double,
        val previousAmount: Double,
        val changePercentage: Double
    ) : BudgetInsight
}
