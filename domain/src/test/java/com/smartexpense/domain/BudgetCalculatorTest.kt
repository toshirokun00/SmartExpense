package com.smartexpense.domain

import com.smartexpense.domain.budget.BudgetCalculator
import com.smartexpense.domain.model.Budget
import com.smartexpense.domain.model.BudgetStatus
import com.smartexpense.domain.model.Expense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class BudgetCalculatorTest {

    private fun dateMillis(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private val budget = Budget(
        id = 1L,
        categoryId = 1L,
        amount = 1_000.0,
        startDate = dateMillis(2026, 10, 1),
        endDate = dateMillis(2026, 10, 31)
    )

    @Test
    fun containsDateIncludesStartAndEndDays() {
        assertTrue(BudgetCalculator.containsDate(budget, dateMillis(2026, 10, 1)))
        assertTrue(BudgetCalculator.containsDate(budget, dateMillis(2026, 10, 31)))
        assertTrue(BudgetCalculator.containsDate(budget, dateMillis(2026, 10, 15)))
    }

    @Test
    fun containsDateExcludesDaysOutsidePeriod() {
        assertFalse(BudgetCalculator.containsDate(budget, dateMillis(2026, 9, 30)))
        assertFalse(BudgetCalculator.containsDate(budget, dateMillis(2026, 11, 1)))
    }

    @Test
    fun spentAmountOnlySumsMatchingCategoryAndPeriod() {
        val expenses = listOf(
            Expense(1L, 250.0, 1L, "in range", dateMillis(2026, 10, 5)),
            Expense(2L, 150.0, 1L, "last day", dateMillis(2026, 10, 31)),
            Expense(3L, 999.0, 2L, "other category", dateMillis(2026, 10, 5)),
            Expense(4L, 999.0, 1L, "out of range", dateMillis(2026, 11, 2))
        )

        assertEquals(400.0, BudgetCalculator.spentAmount(budget, expenses), 0.001)
    }

    @Test
    fun utilizationPercentageIsPreservedAboveOneHundred() {
        assertEquals(40.0, BudgetCalculator.utilizationPercentage(400.0, 1_000.0), 0.001)
        assertEquals(120.0, BudgetCalculator.utilizationPercentage(1_200.0, 1_000.0), 0.001)
    }

    @Test
    fun utilizationPercentageIsZeroForNonPositiveLimit() {
        assertEquals(0.0, BudgetCalculator.utilizationPercentage(500.0, 0.0), 0.001)
        assertEquals(0.0, BudgetCalculator.utilizationPercentage(500.0, -10.0), 0.001)
    }

    @Test
    fun remainingAmountCanBeNegative() {
        assertEquals(600.0, BudgetCalculator.remainingAmount(400.0, 1_000.0), 0.001)
        assertEquals(-200.0, BudgetCalculator.remainingAmount(1_200.0, 1_000.0), 0.001)
    }

    @Test
    fun progressIsCappedAtOne() {
        assertEquals(0.4f, BudgetCalculator.progress(400.0, 1_000.0), 0.001f)
        assertEquals(1f, BudgetCalculator.progress(1_200.0, 1_000.0), 0.001f)
        assertEquals(0f, BudgetCalculator.progress(500.0, 0.0), 0.001f)
    }

    @Test
    fun statusReflectsWarningThreshold() {
        assertEquals(BudgetStatus.WITHIN_BUDGET, BudgetCalculator.status(79.9, 80))
        assertEquals(BudgetStatus.APPROACHING_LIMIT, BudgetCalculator.status(80.0, 80))
        assertEquals(BudgetStatus.APPROACHING_LIMIT, BudgetCalculator.status(99.9, 80))
        assertEquals(BudgetStatus.EXCEEDED, BudgetCalculator.status(100.0, 80))
        assertEquals(BudgetStatus.EXCEEDED, BudgetCalculator.status(150.0, 80))
    }

    @Test
    fun utilizationBuildsCompleteSummary() {
        val expenses = listOf(
            Expense(1L, 850.0, 1L, "food", dateMillis(2026, 10, 10))
        )

        val utilization = BudgetCalculator.utilization(budget, expenses, 80)

        assertEquals(850.0, utilization.spentAmount, 0.001)
        assertEquals(150.0, utilization.remainingAmount, 0.001)
        assertEquals(85.0, utilization.utilizationPercentage, 0.001)
        assertEquals(0.85f, utilization.progress, 0.001f)
        assertEquals(BudgetStatus.APPROACHING_LIMIT, utilization.status)
        assertFalse(utilization.isOverBudget)
        assertEquals(0.0, utilization.overBudgetAmount, 0.001)
    }

    @Test
    fun utilizationExposesOverBudgetAmount() {
        val expenses = listOf(
            Expense(1L, 1_200.0, 1L, "food", dateMillis(2026, 10, 10))
        )

        val utilization = BudgetCalculator.utilization(budget, expenses, 80)

        assertEquals(BudgetStatus.EXCEEDED, utilization.status)
        assertTrue(utilization.isOverBudget)
        assertEquals(200.0, utilization.overBudgetAmount, 0.001)
        assertEquals(1f, utilization.progress, 0.001f)
    }
}
