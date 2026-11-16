package com.smartexpense.paycheck

import com.smartexpense.paycheck.util.billMonthSections
import com.smartexpense.paycheck.util.monthHeader
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class BillMonthSectionsTest {

    private val zone = ZoneOffset.UTC

    private data class Row(val dueDateMillis: Long, val amount: Double)

    private fun millis(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()

    @Test
    fun `rows are grouped by the month of their due date`() {
        val rows = listOf(
            Row(millis(2026, 11, 15), 100.0),
            Row(millis(2026, 11, 28), 50.0),
            Row(millis(2026, 12, 2), 75.0)
        )

        val sections = billMonthSections(
            rows = rows,
            zoneId = zone,
            payDateMillis = millis(2026, 11, 15),
            dueDateMillis = { it.dueDateMillis },
            amount = { it.amount }
        )

        assertEquals(
            listOf(YearMonth.of(2026, 11), YearMonth.of(2026, 12)),
            sections.map { it.month }
        )
        assertEquals(listOf(100.0, 50.0), sections[0].rows.map { it.amount })
        assertEquals(150.0, sections[0].total, 0.001)
        assertEquals(75.0, sections[1].total, 0.001)
    }

    @Test
    fun `a month is named for the paycheck that pays it`() {
        val payMonth = YearMonth.of(2026, 11)

        assertEquals(
            "Bills for November 2026",
            monthHeader(YearMonth.of(2026, 11), payMonth)
        )
        assertEquals(
            "Bills for October 2026",
            monthHeader(YearMonth.of(2026, 10), payMonth)
        )
        assertEquals(
            "Bills covered for December 2026",
            monthHeader(YearMonth.of(2026, 12), payMonth)
        )
    }
}
