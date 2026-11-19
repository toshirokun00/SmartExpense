package com.smartexpense.bills

import com.smartexpense.bills.util.billMonthLabel
import com.smartexpense.bills.util.groupBillsByMonth
import com.smartexpense.domain.model.Bill
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class BillListGroupingTest {

    private val zone = ZoneOffset.UTC
    private val clock = Clock.fixed(
        Instant.parse("2026-11-17T00:00:00Z"),
        zone
    )

    private fun millis(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()

    private fun bill(
        id: Long,
        dueDate: Long,
        amount: Double = 100.0
    ) = Bill(
        id = id,
        name = "Bill $id",
        amount = amount,
        dueDateMillis = dueDate
    )

    @Test
    fun `bills are grouped into months by due date, oldest first`() {
        val bills = listOf(
            bill(id = 3, dueDate = millis(2026, 12, 10), amount = 75.0),
            bill(id = 1, dueDate = millis(2026, 11, 30), amount = 100.0),
            bill(id = 2, dueDate = millis(2026, 11, 15), amount = 50.0),
            bill(id = 4, dueDate = millis(2027, 1, 5), amount = 25.0)
        )

        val months = groupBillsByMonth(bills, clock)

        assertEquals(
            listOf(
                YearMonth.of(2026, 11),
                YearMonth.of(2026, 12),
                YearMonth.of(2027, 1)
            ),
            months.map { it.yearMonth }
        )
        // Within a month the earliest due date comes first.
        assertEquals(listOf(2L, 1L), months[0].bills.map { it.id })
        assertEquals(2, months[0].count)
        assertEquals(150.0, months[0].total, 0.001)
        assertEquals(75.0, months[1].total, 0.001)
        assertEquals(25.0, months[2].total, 0.001)
    }

    @Test
    fun `the current and next month are named relative to today`() {
        val current = YearMonth.of(2026, 11)

        assertEquals(
            "This month • November 2026",
            billMonthLabel(current, current)
        )
        assertEquals(
            "Next month • December 2026",
            billMonthLabel(current.plusMonths(1), current)
        )
        assertEquals(
            "October 2026",
            billMonthLabel(current.minusMonths(1), current)
        )
        assertEquals(
            "January 2027",
            billMonthLabel(current.plusMonths(2), current)
        )
    }

    @Test
    fun `months carry the current and next labels from the clock`() {
        val bills = listOf(
            bill(id = 1, dueDate = millis(2026, 11, 30)),
            bill(id = 2, dueDate = millis(2026, 12, 2))
        )

        val months = groupBillsByMonth(bills, clock)

        assertEquals("This month • November 2026", months[0].label)
        assertEquals("Next month • December 2026", months[1].label)
    }

    @Test
    fun `no bills produces no month groups`() {
        assertEquals(emptyList<Any>(), groupBillsByMonth(emptyList(), clock))
    }
}
