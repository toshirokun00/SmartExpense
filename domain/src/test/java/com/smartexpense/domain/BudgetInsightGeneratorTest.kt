package com.smartexpense.domain

import com.smartexpense.domain.budget.BudgetInsightGenerator
import com.smartexpense.domain.model.Budget
import com.smartexpense.domain.model.BudgetInsight
import com.smartexpense.domain.model.Category
import com.smartexpense.domain.model.Expense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class BudgetInsightGeneratorTest {

    private fun dateMillis(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private val categories = listOf(Category(1L, "Food"))

    private val octoberBudget = Budget(
        id = 1L,
        categoryId = 1L,
        amount = 1_000.0,
        startDate = dateMillis(2026, 10, 1),
        endDate = dateMillis(2026, 10, 10)
    )

    private fun insightOfType(
        insights: List<BudgetInsight>,
        predicate: (BudgetInsight) -> Boolean
    ) = insights.firstOrNull(predicate)

    @Test
    fun approachingLimitInsightIsProduced() {
        val insights = BudgetInsightGenerator.generate(
            budgets = listOf(octoberBudget),
            expenses = listOf(Expense(1L, 850.0, 1L, "food", dateMillis(2026, 10, 5))),
            categories = categories,
            warningThreshold = 80,
            now = dateMillis(2026, 10, 10)
        )

        val insight = insightOfType(insights) { it is BudgetInsight.ApproachingLimit }
        assertTrue(insight is BudgetInsight.ApproachingLimit)
        insight as BudgetInsight.ApproachingLimit
        assertEquals("Food", insight.categoryName)
        assertEquals(85.0, insight.utilizationPercentage, 0.001)
        assertEquals(150.0, insight.remainingAmount, 0.001)
    }

    @Test
    fun exceededLimitInsightIsProduced() {
        val insights = BudgetInsightGenerator.generate(
            budgets = listOf(octoberBudget),
            expenses = listOf(Expense(1L, 1_200.0, 1L, "food", dateMillis(2026, 10, 5))),
            categories = categories,
            warningThreshold = 80,
            now = dateMillis(2026, 10, 10)
        )

        val insight = insightOfType(insights) { it is BudgetInsight.ExceededLimit }
        assertTrue(insight is BudgetInsight.ExceededLimit)
        insight as BudgetInsight.ExceededLimit
        assertEquals("Food", insight.categoryName)
        assertEquals(200.0, insight.overAmount, 0.001)
    }

    @Test
    fun spendingTrendComparesCurrentWithEquivalentPreviousPeriod() {
        val insights = BudgetInsightGenerator.generate(
            budgets = listOf(octoberBudget),
            expenses = listOf(
                Expense(1L, 300.0, 1L, "current", dateMillis(2026, 10, 5)),
                Expense(2L, 200.0, 1L, "previous", dateMillis(2026, 9, 25))
            ),
            categories = categories,
            warningThreshold = 80,
            now = dateMillis(2026, 10, 10)
        )

        val trend = insightOfType(insights) { it is BudgetInsight.SpendingTrend }
        assertTrue(trend is BudgetInsight.SpendingTrend)
        trend as BudgetInsight.SpendingTrend
        assertEquals(300.0, trend.currentAmount, 0.001)
        assertEquals(200.0, trend.previousAmount, 0.001)
        assertEquals(50.0, trend.changePercentage, 0.001)
        assertTrue(trend.isIncrease)
    }

    @Test
    fun incompletePeriodComparesOnlyEquivalentElapsedPortion() {
        val monthlyBudget = octoberBudget.copy(
            startDate = dateMillis(2026, 10, 1),
            endDate = dateMillis(2026, 10, 31)
        )

        val insights = BudgetInsightGenerator.generate(
            budgets = listOf(monthlyBudget),
            expenses = listOf(
                // current elapsed window: Oct 1 .. Oct 10
                Expense(1L, 500.0, 1L, "current", dateMillis(2026, 10, 10)),
                // equivalent previous window: Aug 31 .. Sep 9
                Expense(2L, 250.0, 1L, "prev-equivalent", dateMillis(2026, 9, 9)),
                // later in the previous month, but outside the comparable window
                Expense(3L, 999.0, 1L, "prev-outside", dateMillis(2026, 9, 20))
            ),
            categories = categories,
            warningThreshold = 80,
            now = dateMillis(2026, 10, 10)
        )

        val trend = insightOfType(insights) { it is BudgetInsight.SpendingTrend }
        assertTrue(trend is BudgetInsight.SpendingTrend)
        trend as BudgetInsight.SpendingTrend
        assertEquals(500.0, trend.currentAmount, 0.001)
        assertEquals(250.0, trend.previousAmount, 0.001)
        assertEquals(100.0, trend.changePercentage, 0.001)
    }

    @Test
    fun categorySpendingChangeIsFlaggedWhenIncreaseIsLarge() {
        val insights = BudgetInsightGenerator.generate(
            budgets = listOf(octoberBudget),
            expenses = listOf(
                Expense(1L, 300.0, 1L, "current", dateMillis(2026, 10, 5)),
                Expense(2L, 100.0, 1L, "previous", dateMillis(2026, 9, 25))
            ),
            categories = categories,
            warningThreshold = 80,
            now = dateMillis(2026, 10, 10)
        )

        val change = insightOfType(insights) { it is BudgetInsight.CategorySpendingChange }
        assertTrue(change is BudgetInsight.CategorySpendingChange)
        change as BudgetInsight.CategorySpendingChange
        assertEquals("Food", change.categoryName)
        assertEquals(200.0, change.changePercentage, 0.001)
    }

    @Test
    fun noTrendOrCategoryChangeWhenPreviousPeriodIsEmpty() {
        val insights = BudgetInsightGenerator.generate(
            budgets = listOf(octoberBudget),
            expenses = listOf(
                Expense(1L, 300.0, 1L, "current", dateMillis(2026, 10, 5))
            ),
            categories = categories,
            warningThreshold = 80,
            now = dateMillis(2026, 10, 10)
        )

        assertTrue(insights.none { it is BudgetInsight.SpendingTrend })
        assertTrue(insights.none { it is BudgetInsight.CategorySpendingChange })
    }

    @Test
    fun insufficientHistoryProducesNoComparisons() {
        val insights = BudgetInsightGenerator.generate(
            budgets = listOf(octoberBudget),
            expenses = emptyList(),
            categories = categories,
            warningThreshold = 80,
            now = dateMillis(2026, 10, 10)
        )

        assertTrue(insights.isEmpty())
    }

    @Test
    fun emptyBudgetsProduceNoInsights() {
        val insights = BudgetInsightGenerator.generate(
            budgets = emptyList(),
            expenses = listOf(Expense(1L, 5_000.0, 1L, "food", dateMillis(2026, 10, 5))),
            categories = categories,
            warningThreshold = 80,
            now = dateMillis(2026, 10, 10)
        )

        assertTrue(insights.isEmpty())
    }

    @Test
    fun duplicateBudgetsForSameCategoryProduceDistinctInsightIdentities() {
        val duplicateBudget = octoberBudget.copy(id = 2L)

        val insights = BudgetInsightGenerator.generate(
            budgets = listOf(octoberBudget, duplicateBudget),
            expenses = listOf(
                Expense(1L, 850.0, 1L, "current", dateMillis(2026, 10, 5)),
                Expense(2L, 100.0, 1L, "previous", dateMillis(2026, 9, 25))
            ),
            categories = categories,
            warningThreshold = 80,
            now = dateMillis(2026, 10, 10)
        )

        val approaching = insights
            .filterIsInstance<BudgetInsight.ApproachingLimit>()

        assertEquals(
            listOf(1L, 2L),
            approaching.map { it.budgetId }
        )
        assertEquals(
            approaching.size,
            approaching.map { it.budgetId }.distinct().size
        )

        val categoryChanges = insights
            .filterIsInstance<BudgetInsight.CategorySpendingChange>()

        assertEquals(1, categoryChanges.size)
        assertEquals(1L, categoryChanges.first().categoryId)
    }

    @Test
    fun notYetStartedPeriodProducesNoComparisons() {
        val futureBudget = octoberBudget.copy(
            startDate = dateMillis(2026, 12, 1),
            endDate = dateMillis(2026, 12, 31)
        )

        val insights = BudgetInsightGenerator.generate(
            budgets = listOf(futureBudget),
            expenses = listOf(Expense(1L, 500.0, 1L, "food", dateMillis(2026, 12, 5))),
            categories = categories,
            warningThreshold = 80,
            now = dateMillis(2026, 10, 10)
        )

        assertTrue(insights.none { it is BudgetInsight.SpendingTrend })
        assertTrue(insights.none { it is BudgetInsight.CategorySpendingChange })
    }
}
