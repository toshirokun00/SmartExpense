package com.smartexpense.domain

import com.smartexpense.domain.budget.BudgetAlertEvaluator
import com.smartexpense.domain.model.Budget
import com.smartexpense.domain.model.BudgetAlertRecord
import com.smartexpense.domain.model.Expense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class BudgetAlertEvaluatorTest {

    private fun dateMillis(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private val start = dateMillis(2026, 10, 1)
    private val end = dateMillis(2026, 10, 31)

    private val budget = Budget(
        id = 1L,
        categoryId = 1L,
        amount = 1_000.0,
        startDate = start,
        endDate = end
    )

    private fun expense(amount: Double): Expense =
        Expense(1L, amount, 1L, "food", dateMillis(2026, 10, 10))

    @Test
    fun crossingThresholdProducesAlertAndDeliveredRecord() {
        val result = BudgetAlertEvaluator.evaluate(
            budgets = listOf(budget),
            expenses = listOf(expense(850.0)),
            thresholds = setOf(80, 100),
            previouslyDelivered = emptySet()
        )

        assertEquals(1, result.alerts.size)
        assertEquals(80, result.alerts.first().threshold)
        assertEquals(85.0, result.alerts.first().utilizationPercentage, 0.001)
        assertEquals(
            listOf(BudgetAlertRecord(1L, start, end, 80)),
            result.delivered
        )
        assertTrue(result.removeDelivered.isEmpty())
    }

    @Test
    fun spendingBelowEveryThresholdProducesNoAlert() {
        val result = BudgetAlertEvaluator.evaluate(
            budgets = listOf(budget),
            expenses = listOf(expense(100.0)),
            thresholds = setOf(80, 100),
            previouslyDelivered = emptySet()
        )

        assertTrue(result.alerts.isEmpty())
        assertTrue(result.delivered.isEmpty())
        assertTrue(result.removeDelivered.isEmpty())
    }

    @Test
    fun sameBudgetPeriodAndThresholdIsNotDeliveredTwice() {
        val delivered = setOf(BudgetAlertRecord(1L, start, end, 80))

        val result = BudgetAlertEvaluator.evaluate(
            budgets = listOf(budget),
            expenses = listOf(expense(850.0)),
            thresholds = setOf(80, 100),
            previouslyDelivered = delivered
        )

        assertTrue(result.alerts.isEmpty())
        assertTrue(result.delivered.isEmpty())
    }

    @Test
    fun exceedingOneHundredPercentEmitsBothCrossings() {
        val result = BudgetAlertEvaluator.evaluate(
            budgets = listOf(budget),
            expenses = listOf(expense(1_200.0)),
            thresholds = setOf(80, 100),
            previouslyDelivered = emptySet()
        )

        assertEquals(listOf(80, 100), result.alerts.map { it.threshold })
        assertEquals(2, result.delivered.size)
    }

    @Test
    fun onlyNewlyCrossedThresholdsAlertWhenOneWasAlreadyDelivered() {
        val delivered = setOf(BudgetAlertRecord(1L, start, end, 80))

        val result = BudgetAlertEvaluator.evaluate(
            budgets = listOf(budget),
            expenses = listOf(expense(1_200.0)),
            thresholds = setOf(80, 100),
            previouslyDelivered = delivered
        )

        assertEquals(listOf(100), result.alerts.map { it.threshold })
        assertEquals(listOf(BudgetAlertRecord(1L, start, end, 100)), result.delivered)
    }

    @Test
    fun droppingBelowThresholdRemovesRecordSoItCanAlertAgain() {
        val delivered = setOf(BudgetAlertRecord(1L, start, end, 80))

        val result = BudgetAlertEvaluator.evaluate(
            budgets = listOf(budget),
            expenses = listOf(expense(200.0)),
            thresholds = setOf(80, 100),
            previouslyDelivered = delivered
        )

        assertTrue(result.alerts.isEmpty())
        assertEquals(
            listOf(BudgetAlertRecord(1L, start, end, 80)),
            result.removeDelivered
        )

        val afterReCross = BudgetAlertEvaluator.evaluate(
            budgets = listOf(budget),
            expenses = listOf(expense(900.0)),
            thresholds = setOf(80, 100),
            previouslyDelivered = emptySet()
        )
        assertEquals(listOf(80), afterReCross.alerts.map { it.threshold })
    }

    @Test
    fun periodRolloverUsesNewKeyAndAlertsAgain() {
        val rolled = budget.copy(
            startDate = dateMillis(2026, 11, 1),
            endDate = dateMillis(2026, 11, 30)
        )
        val delivered = setOf(BudgetAlertRecord(1L, start, end, 80))

        val result = BudgetAlertEvaluator.evaluate(
            budgets = listOf(rolled),
            expenses = listOf(
                Expense(2L, 900.0, 1L, "food", dateMillis(2026, 11, 5))
            ),
            thresholds = setOf(80),
            previouslyDelivered = delivered
        )

        assertEquals(listOf(80), result.alerts.map { it.threshold })
        assertEquals(
            listOf(BudgetAlertRecord(1L, rolled.startDate, rolled.endDate, 80)),
            result.delivered
        )
        assertTrue(
            result.removeDelivered.contains(BudgetAlertRecord(1L, start, end, 80))
        )
    }

    @Test
    fun deletedBudgetRecordsArePurged() {
        val delivered = setOf(BudgetAlertRecord(1L, start, end, 80))

        val result = BudgetAlertEvaluator.evaluate(
            budgets = emptyList(),
            expenses = emptyList(),
            thresholds = setOf(80),
            previouslyDelivered = delivered
        )

        assertTrue(result.alerts.isEmpty())
        assertEquals(
            listOf(BudgetAlertRecord(1L, start, end, 80)),
            result.removeDelivered
        )
    }
}
