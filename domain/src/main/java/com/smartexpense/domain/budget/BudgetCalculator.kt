package com.smartexpense.domain.budget

import com.smartexpense.domain.model.Budget
import com.smartexpense.domain.model.BudgetStatus
import com.smartexpense.domain.model.BudgetUtilization
import com.smartexpense.domain.model.Expense
import java.time.Instant
import java.time.ZoneId

/**
 * Pure, testable budget math shared by the dashboard, budget screen and alerts.
 *
 * Spending period boundaries intentionally mirror the existing app behaviour: a
 * budget period is inclusive of both its start and end *calendar* dates, evaluated
 * in the device's default time zone.
 */
object BudgetCalculator {

    /**
     * Whether [timestamp] falls within the budget period (inclusive of the start
     * and end calendar days).
     */
    fun containsDate(
        budget: Budget,
        timestamp: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Boolean {
        val date = toLocalDate(timestamp, zoneId)
        val start = toLocalDate(budget.startDate, zoneId)
        val end = toLocalDate(budget.endDate, zoneId)
        return date in start..end
    }

    /** Total spending in [expenses] that belongs to [budget]'s category and period. */
    fun spentAmount(
        budget: Budget,
        expenses: List<Expense>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Double {
        return expenses
            .filter { expense ->
                expense.categoryId == budget.categoryId &&
                    containsDate(budget, expense.date, zoneId)
            }
            .sumOf { it.amount }
    }

    /** Remaining budget = limit - spent (may be negative when exceeded). */
    fun remainingAmount(spentAmount: Double, limit: Double): Double {
        return limit - spentAmount
    }

    /**
     * Utilization as a percentage, preserved above 100%. Returns 0.0 for a
     * non-positive limit, matching the existing "invalid amount" validation that
     * forbids budgets of zero or less.
     */
    fun utilizationPercentage(spentAmount: Double, limit: Double): Double {
        return if (limit > 0.0) {
            spentAmount / limit * 100.0
        } else {
            0.0
        }
    }

    /** Utilization clamped to 0..1 for visual progress indicators. */
    fun progress(spentAmount: Double, limit: Double): Float {
        return if (limit > 0.0) {
            (spentAmount / limit).coerceIn(0.0, 1.0).toFloat()
        } else {
            0f
        }
    }

    /** Derives the status from the uncapped utilization and a warning threshold. */
    fun status(utilizationPercentage: Double, warningThreshold: Int): BudgetStatus {
        return when {
            utilizationPercentage >= 100.0 -> BudgetStatus.EXCEEDED
            utilizationPercentage >= warningThreshold -> BudgetStatus.APPROACHING_LIMIT
            else -> BudgetStatus.WITHIN_BUDGET
        }
    }

    /** Computes the full utilization summary for a single budget. */
    fun utilization(
        budget: Budget,
        expenses: List<Expense>,
        warningThreshold: Int,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): BudgetUtilization {
        val spent = spentAmount(budget, expenses, zoneId)
        val utilization = utilizationPercentage(spent, budget.amount)
        return BudgetUtilization(
            budget = budget,
            spentAmount = spent,
            remainingAmount = remainingAmount(spent, budget.amount),
            utilizationPercentage = utilization,
            progress = progress(spent, budget.amount),
            status = status(utilization, warningThreshold)
        )
    }

    private fun toLocalDate(
        timestamp: Long,
        zoneId: ZoneId
    ) = Instant.ofEpochMilli(timestamp)
        .atZone(zoneId)
        .toLocalDate()
}
