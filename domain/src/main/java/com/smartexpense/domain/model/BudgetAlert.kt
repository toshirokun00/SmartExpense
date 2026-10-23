package com.smartexpense.domain.model

/**
 * A utilization threshold that has just been crossed for a budget period.
 */
data class BudgetAlert(
    val budgetId: Long,
    val categoryId: Long,
    val threshold: Int,
    val spentAmount: Double,
    val budgetAmount: Double,
    val utilizationPercentage: Double
)

/**
 * Delivery history used to deduplicate budget alerts.
 *
 * An alert is considered delivered for a budget, its period and a threshold once a
 * matching record is persisted. The period is part of the key so that a budget
 * which rolls over to a new period (or changes its date range) can alert again.
 */
data class BudgetAlertRecord(
    val budgetId: Long,
    val periodStart: Long,
    val periodEnd: Long,
    val threshold: Int
)

/**
 * The configured, selectable budget alert thresholds.
 */
object BudgetAlertThresholds {

    /** Thresholds offered in settings. */
    val SUPPORTED: List<Int> = listOf(50, 80, 90, 100)

    /**
     * Defaults used when the user has not configured anything: every supported
     * threshold is enabled.
     */
    val DEFAULT: Set<Int> = SUPPORTED.toSet()

    /** Default threshold above which a budget is considered "approaching the limit". */
    const val WARNING: Int = 80

    /**
     * The effective warning threshold used for the "approaching limit" status and
     * insight: the lowest configured threshold below 100, falling back to
     * [WARNING] when none is configured.
     */
    fun warningThreshold(thresholds: Set<Int>): Int {
        return thresholds.filter { it in 1..99 }.minOrNull() ?: WARNING
    }

    /**
     * Keeps only supported, positive thresholds.
     *
     * An empty result is a valid configuration meaning "no budget alerts".
     * Callers that need defaults because the user has never configured
     * thresholds must apply [DEFAULT] themselves.
     */
    fun normalize(thresholds: Set<Int>): Set<Int> {
        return thresholds.filter { it in SUPPORTED }.toSet()
    }
}
