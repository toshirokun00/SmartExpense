package com.smartexpense.domain

import com.smartexpense.domain.model.Bill
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.model.ExpenseType
import com.smartexpense.domain.model.Paycheck
import com.smartexpense.domain.model.buildPaycheckEnvelopes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class PaycheckEnvelopeTest {

    private val zone = ZoneOffset.UTC

    private fun millis(year: Int, month: Int, day: Int, hour: Int = 0): Long =
        LocalDateTime.of(year, month, day, hour, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

    private fun dateMillis(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()

    private fun paycheck(
        id: Long,
        year: Int,
        month: Int,
        day: Int,
        amount: Double = 1_000.0,
        billIds: List<Long> = emptyList()
    ) = Paycheck(
        id = id,
        amount = amount,
        payDateMillis = dateMillis(year, month, day),
        billIds = billIds
    )

    private fun bill(id: Long, amount: Double) = Bill(
        id = id,
        name = "Bill $id",
        amount = amount,
        dueDateMillis = 0L
    )

    private fun expense(
        year: Int,
        month: Int,
        day: Int,
        amount: Double,
        hour: Int = 12,
        type: ExpenseType = ExpenseType.EXPENSE
    ) = Expense(
        amount = amount,
        categoryId = 1L,
        description = "Expense",
        date = millis(year, month, day, hour),
        expenseType = type
    )

    @Test
    fun `expenses belong to the cycle between paydays`() {
        val paychecks = listOf(
            paycheck(1L, 2026, 11, 15, billIds = listOf(1L)),
            paycheck(2L, 2026, 11, 30, billIds = listOf(2L))
        )
        val bills = listOf(bill(1L, 200.0), bill(2L, 300.0))
        val expenses = listOf(
            expense(2026, 11, 14, 500.0),
            expense(2026, 11, 15, 100.0),
            expense(2026, 11, 29, 50.0),
            expense(2026, 11, 30, 75.0),
            expense(2026, 12, 10, 25.0),
            expense(2026, 12, 30, 999.0)
        )

        val envelopes = buildPaycheckEnvelopes(
            paychecks = paychecks,
            bills = bills,
            expenses = expenses,
            nowMillis = millis(2026, 11, 13),
            zoneId = zone
        )

        assertEquals(2, envelopes.size)
        val first = envelopes[0]
        assertEquals(200.0, first.billsTotal, 0.001)
        assertEquals(listOf(100.0, 50.0), first.expenses.map { it.amount })
        assertEquals(150.0, first.expensesTotal, 0.001)
        assertEquals(650.0, first.remaining, 0.001)
        assertEquals(dateMillis(2026, 11, 15), first.cycleStartMillis)
        assertEquals(dateMillis(2026, 11, 30), first.cycleEndMillis)
        assertEquals(LocalDate.of(2026, 11, 29), first.cycleLastDate(zone))

        val second = envelopes[1]
        assertEquals(300.0, second.billsTotal, 0.001)
        assertEquals(listOf(75.0, 25.0), second.expenses.map { it.amount })
        assertEquals(600.0, second.remaining, 0.001)
        assertEquals(dateMillis(2026, 12, 30), second.cycleEndMillis)
    }

    @Test
    fun `only real expenses are deducted`() {
        val paychecks = listOf(paycheck(1L, 2026, 11, 15))
        val expenses = listOf(
            expense(2026, 11, 16, 100.0),
            expense(2026, 11, 17, 500.0, type = ExpenseType.INCOME),
            expense(2026, 11, 18, 50.0, type = ExpenseType.REFUND)
        )

        val envelope = buildPaycheckEnvelopes(
            paychecks = paychecks,
            bills = emptyList(),
            expenses = expenses,
            nowMillis = millis(2026, 11, 13),
            zoneId = zone
        ).single()

        assertEquals(100.0, envelope.expensesTotal, 0.001)
        assertEquals(900.0, envelope.remaining, 0.001)
    }

    @Test
    fun `hidden future paychecks still bound the previous cycle`() {
        val paychecks = listOf(
            paycheck(1L, 2026, 12, 31),
            paycheck(2L, 2027, 2, 15)
        )

        val envelopes = buildPaycheckEnvelopes(
            paychecks = paychecks,
            bills = emptyList(),
            expenses = emptyList(),
            nowMillis = millis(2026, 11, 13),
            zoneId = zone
        )

        // February is beyond the current/next-month window.
        assertEquals(1, envelopes.size)
        assertEquals(1L, envelopes.single().paycheck.id)
        assertEquals(dateMillis(2027, 2, 15), envelopes.single().cycleEndMillis)
    }

    @Test
    fun `a lone paycheck falls back to a one month cycle`() {
        val envelopes = buildPaycheckEnvelopes(
            paychecks = listOf(paycheck(1L, 2026, 11, 15)),
            bills = emptyList(),
            expenses = emptyList(),
            nowMillis = millis(2026, 11, 13),
            zoneId = zone
        )

        assertEquals(dateMillis(2026, 12, 15), envelopes.single().cycleEndMillis)
    }

    @Test
    fun `missing assigned bills are ignored`() {
        val envelopes = buildPaycheckEnvelopes(
            paychecks = listOf(
                paycheck(1L, 2026, 11, 15, billIds = listOf(1L, 99L))
            ),
            bills = listOf(bill(1L, 250.0)),
            expenses = emptyList(),
            nowMillis = millis(2026, 11, 13),
            zoneId = zone
        )

        assertEquals(1, envelopes.single().assignedBills.size)
        assertEquals(250.0, envelopes.single().billsTotal, 0.001)
    }

    @Test
    fun `an overspent cycle is reported as short`() {
        val envelopes = buildPaycheckEnvelopes(
            paychecks = listOf(
                paycheck(
                    id = 1L,
                    year = 2026,
                    month = 11,
                    day = 15,
                    amount = 500.0,
                    billIds = listOf(1L)
                )
            ),
            bills = listOf(bill(1L, 400.0)),
            expenses = listOf(expense(2026, 11, 16, 150.0)),
            nowMillis = millis(2026, 11, 13),
            zoneId = zone
        )

        val envelope = envelopes.single()
        assertTrue(envelope.isShort)
        assertEquals(-50.0, envelope.remaining, 0.001)
        assertEquals(1f, envelope.usedFraction)
    }

    @Test
    fun `past paychecks remain visible`() {
        val envelopes = buildPaycheckEnvelopes(
            paychecks = listOf(
                paycheck(1L, 2025, 1, 15),
                paycheck(2L, 2026, 11, 15)
            ),
            bills = emptyList(),
            expenses = emptyList(),
            nowMillis = millis(2026, 11, 13),
            zoneId = zone
        )

        assertEquals(listOf(1L, 2L), envelopes.map { it.paycheck.id })
        assertFalse(envelopes.single { it.paycheck.id == 1L }.isShort)
    }
}
