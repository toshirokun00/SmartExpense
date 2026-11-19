package com.smartexpense.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The money plan for one paycheck cycle.
 *
 * A cycle runs from a paycheck's pay date up to the day before the next
 * paycheck's pay date. Assigned bills and expenses recorded inside that window
 * are deducted from the paycheck amount, leaving the money available for the
 * cutoff.
 */
data class PaycheckEnvelope(
    val paycheck: Paycheck,
    /** Bills the user assigned to this paycheck. */
    val assignedBills: List<Bill> = emptyList(),
    /** Expenses recorded during this paycheck's cycle. */
    val expenses: List<Expense> = emptyList(),
    /** Start of the cycle, inclusive. */
    val cycleStartMillis: Long,
    /** Start of the next cycle, exclusive. */
    val cycleEndMillis: Long
) {
    val income: Double get() = paycheck.amount

    val billsTotal: Double get() = assignedBills.sumOf { it.amount }

    val expensesTotal: Double get() = expenses.sumOf { it.amount }

    val totalDeductions: Double get() = billsTotal + expensesTotal

    /** Money left for this cutoff. Negative means the plan is short. */
    val remaining: Double get() = income - totalDeductions

    val isShort: Boolean get() = remaining < 0

    /** Share of the paycheck already covered by bills and expenses. */
    val usedFraction: Float
        get() = when {
            income > 0 -> (totalDeductions / income).toFloat().coerceIn(0f, 1f)
            totalDeductions > 0 -> 1f
            else -> 0f
        }

    fun cycleStartDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(cycleStartMillis).atZone(zoneId).toLocalDate()

    fun cycleEndExclusive(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(cycleEndMillis).atZone(zoneId).toLocalDate()

    /** Last calendar day included in this cycle. */
    fun cycleLastDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
        cycleEndExclusive(zoneId).minusDays(1)
}

/**
 * Builds one [PaycheckEnvelope] per visible paycheck.
 *
 * The full paycheck list is used to find cycle boundaries, so a paycheck whose
 * next cutoff is outside the visible window still gets the correct end date.
 * Only expenses of type [ExpenseType.EXPENSE] are deducted; income and refunds
 * are not treated as spending.
 *
 * The returned list keeps the visible window used by the paycheck list: all
 * past paychecks, the current month, and next month.
 */
fun buildPaycheckEnvelopes(
    paychecks: List<Paycheck>,
    bills: List<Bill>,
    expenses: List<Expense>,
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): List<PaycheckEnvelope> {
    if (paychecks.isEmpty()) return emptyList()

    val sortedPaychecks = paychecks.sortedBy { it.payDateMillis }
    val billsById = bills.associateBy { it.id }

    val envelopes = sortedPaychecks.mapIndexed { index, paycheck ->
        val cycleStartDate = paycheck.payDate(zoneId)
        val nextPayDate = sortedPaychecks
            .drop(index + 1)
            .firstOrNull { it.payDateMillis > paycheck.payDateMillis }
            ?.payDate(zoneId)
            ?: cycleStartDate.plusMonths(1)

        val cycleStart = cycleStartDate
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        val cycleEnd = nextPayDate
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

        val cycleExpenses = expenses
            .filter { expense ->
                expense.expenseType == ExpenseType.EXPENSE &&
                    expense.date >= cycleStart &&
                    expense.date < cycleEnd
            }
            .sortedBy { it.date }

        PaycheckEnvelope(
            paycheck = paycheck,
            assignedBills = paycheck.billIds
                .distinct()
                .mapNotNull { billsById[it] },
            expenses = cycleExpenses,
            cycleStartMillis = cycleStart,
            cycleEndMillis = cycleEnd
        )
    }

    val visible = visiblePaychecks(sortedPaychecks, nowMillis, zoneId).toSet()
    return envelopes.filter { it.paycheck in visible }
}
