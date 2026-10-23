package com.smartexpense.domain.budget

import com.smartexpense.domain.model.Budget
import com.smartexpense.domain.model.BudgetInsight
import com.smartexpense.domain.model.BudgetStatus
import com.smartexpense.domain.model.Category
import com.smartexpense.domain.model.Expense
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Deterministic, locally-computed spending insights.
 *
 * All comparisons are made against an equivalent, equally-elapsed portion of the
 * previous period so that an in-progress current period is not compared unfairly
 * against a full previous period. Insights are omitted whenever there is not
 * enough comparable history (empty history, zero previous spending, invalid or
 * not-yet-started periods).
 */
object BudgetInsightGenerator {

    /** Minimum increase over the previous comparable period to flag a category. */
    private const val CATEGORY_SPIKE_PERCENTAGE = 50.0

    fun generate(
        budgets: List<Budget>,
        expenses: List<Expense>,
        categories: List<Category>,
        warningThreshold: Int,
        now: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<BudgetInsight> {

        if (budgets.isEmpty()) return emptyList()

        val nowLocalDate = toLocalDate(now, zoneId)
        val insights = mutableListOf<BudgetInsight>()

        // 1 & 2: approaching / exceeded budgets.
        budgets.forEach { budget ->
            val utilization = BudgetCalculator.utilization(budget, expenses, warningThreshold, zoneId)
            val categoryName = categoryName(budget, categories)
            when (utilization.status) {
                BudgetStatus.EXCEEDED -> insights += BudgetInsight.ExceededLimit(
                    budgetId = budget.id,
                    categoryName = categoryName,
                    overAmount = utilization.overBudgetAmount,
                    utilizationPercentage = utilization.utilizationPercentage
                )

                BudgetStatus.APPROACHING_LIMIT -> insights += BudgetInsight.ApproachingLimit(
                    budgetId = budget.id,
                    categoryName = categoryName,
                    utilizationPercentage = utilization.utilizationPercentage,
                    remainingAmount = utilization.remainingAmount
                )

                BudgetStatus.WITHIN_BUDGET -> Unit
            }
        }

        // 3: overall spending trend for the most relevant (focused) period.
        val focused = budgets.firstOrNull { BudgetCalculator.containsDate(it, now, zoneId) }
            ?: budgets.maxByOrNull { it.endDate }

        if (focused != null) {
            val period = budgetPeriod(focused, zoneId)?.let { comparableWindows(it, nowLocalDate) }
            if (period != null) {
                val current = spend(expenses, null, period.current, zoneId)
                val previous = spend(expenses, null, period.previous, zoneId)
                if (previous > 0.0) {
                    insights += BudgetInsight.SpendingTrend(
                        currentAmount = current,
                        previousAmount = previous,
                        changePercentage = changePercentage(current, previous),
                        isIncrease = current > previous
                    )
                }
            }
        }

        // 4: category spending spikes versus the previous comparable period.
        //     Multiple budgets can share a category, so keep a single, most
        //     significant insight per category.
        val categoryChanges =
            linkedMapOf<Long, BudgetInsight.CategorySpendingChange>()

        budgets.forEach { budget ->
            val period = budgetPeriod(budget, zoneId)?.let { comparableWindows(it, nowLocalDate) }
                ?: return@forEach

            val current = spend(expenses, budget.categoryId, period.current, zoneId)
            val previous = spend(expenses, budget.categoryId, period.previous, zoneId)

            if (previous > 0.0 && current > previous) {
                val change = changePercentage(current, previous)
                if (change >= CATEGORY_SPIKE_PERCENTAGE) {
                    val candidate = BudgetInsight.CategorySpendingChange(
                        categoryId = budget.categoryId,
                        categoryName = categoryName(budget, categories),
                        currentAmount = current,
                        previousAmount = previous,
                        changePercentage = change
                    )

                    val existing = categoryChanges[budget.categoryId]
                    if (existing == null || change > existing.changePercentage) {
                        categoryChanges[budget.categoryId] = candidate
                    }
                }
            }
        }

        insights += categoryChanges.values

        return insights
    }

    private data class Window(val start: LocalDate, val end: LocalDate)

    private data class ComparablePeriods(val current: Window, val previous: Window)

    private fun budgetPeriod(budget: Budget, zoneId: ZoneId): Window? {
        val start = toLocalDate(budget.startDate, zoneId)
        val end = toLocalDate(budget.endDate, zoneId)
        return if (end < start) null else Window(start, end)
    }

    /**
     * Produces the elapsed portion of the current period and the equivalent elapsed
     * portion of the immediately preceding period of the same length. Returns null
     * when the period is invalid or has not started yet.
     */
    private fun comparableWindows(period: Window, now: LocalDate): ComparablePeriods? {
        if (now < period.start) return null

        val periodDays = ChronoUnit.DAYS.between(period.start, period.end) + 1
        if (periodDays <= 0) return null

        val elapsedEnd = if (now < period.end) now else period.end
        val elapsedDays = ChronoUnit.DAYS.between(period.start, elapsedEnd) + 1

        val previousEnd = period.start.minusDays(1)
        val previousStart = previousEnd.minusDays(periodDays - 1)
        val previousComparableEnd = previousStart.plusDays(elapsedDays - 1)

        return ComparablePeriods(
            current = Window(period.start, elapsedEnd),
            previous = Window(previousStart, previousComparableEnd)
        )
    }

    private fun spend(
        expenses: List<Expense>,
        categoryId: Long?,
        window: Window,
        zoneId: ZoneId
    ): Double {
        return expenses
            .filter { expense ->
                (categoryId == null || expense.categoryId == categoryId) &&
                    toLocalDate(expense.date, zoneId) in window.start..window.end
            }
            .sumOf { it.amount }
    }

    private fun changePercentage(current: Double, previous: Double): Double {
        return if (previous > 0.0) (current - previous) / previous * 100.0 else 0.0
    }

    private fun categoryName(budget: Budget, categories: List<Category>): String {
        return categories.firstOrNull { it.id == budget.categoryId }?.name ?: "Unknown"
    }

    private fun toLocalDate(timestamp: Long, zoneId: ZoneId): LocalDate =
        Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()

    /** Exposed for callers that only need the magnitude of a change. */
    fun absoluteChange(percentage: Double): Double = abs(percentage)
}
