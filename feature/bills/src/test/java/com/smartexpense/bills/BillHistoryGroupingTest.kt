package com.smartexpense.bills

import com.smartexpense.bills.util.formatHistoryCount
import com.smartexpense.bills.util.groupPaymentsByMonth
import com.smartexpense.bills.util.monthLabel
import com.smartexpense.domain.model.BillPayment
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset

class BillHistoryGroupingTest {

    private val clock: Clock = Clock.fixed(
        Instant.parse("2026-11-12T10:00:00Z"),
        ZoneOffset.UTC
    )

    private fun payment(
        id: Long,
        paidAt: String,
        amount: Double = 100.0,
        name: String = "Meralco"
    ) = BillPayment(
        id = id,
        billId = 1L,
        billName = name,
        amount = amount,
        paidAtMillis = Instant.parse(paidAt).toEpochMilli()
    )

    @Test
    fun groupsPaymentsByCalendarMonthNewestFirst() {
        val months = groupPaymentsByMonth(
            listOf(
                payment(1, "2026-09-03T08:00:00Z"),
                payment(2, "2026-11-01T08:00:00Z"),
                payment(3, "2026-10-20T08:00:00Z"),
                payment(4, "2026-11-05T08:00:00Z")
            ),
            clock
        )

        assertEquals(
            listOf(
                YearMonth.of(2026, 11),
                YearMonth.of(2026, 10),
                YearMonth.of(2026, 9)
            ),
            months.map { it.yearMonth }
        )
    }

    @Test
    fun paymentsInsideAMonthAreNewestFirst() {
        val months = groupPaymentsByMonth(
            listOf(
                payment(1, "2026-11-01T08:00:00Z"),
                payment(2, "2026-11-05T08:00:00Z"),
                payment(3, "2026-11-03T08:00:00Z")
            ),
            clock
        )

        assertEquals(listOf(2L, 3L, 1L), months.single().payments.map { it.id })
    }

    @Test
    fun monthTotalsAndCountsAddUp() {
        val months = groupPaymentsByMonth(
            listOf(
                payment(1, "2026-11-01T08:00:00Z", amount = 1000.0),
                payment(2, "2026-11-05T08:00:00Z", amount = 250.5),
                payment(3, "2026-10-05T08:00:00Z", amount = 900.0)
            ),
            clock
        )

        assertEquals(2, months[0].count)
        assertEquals(1250.5, months[0].total, 0.0)
        assertEquals(1, months[1].count)
        assertEquals(900.0, months[1].total, 0.0)
    }

    @Test
    fun labelsNameThisAndLastMonthRelativeToNow() {
        assertEquals(
            "This month • November 2026",
            monthLabel(YearMonth.of(2026, 11), clock)
        )
        assertEquals(
            "Last month • October 2026",
            monthLabel(YearMonth.of(2026, 10), clock)
        )
        assertEquals(
            "September 2026",
            monthLabel(YearMonth.of(2026, 9), clock)
        )
    }

    @Test
    fun monthsCarryAPlainLabelForTheMonthFilter() {
        val months = groupPaymentsByMonth(
            listOf(payment(1, "2026-09-03T08:00:00Z")),
            clock
        )

        assertEquals("September 2026", months.single().shortLabel)
    }

    @Test
    fun labelNamesLastMonthAcrossAYearBoundary() {
        val januaryClock = Clock.fixed(
            Instant.parse("2027-01-15T10:00:00Z"),
            ZoneOffset.UTC
        )

        assertEquals(
            "Last month • December 2026",
            monthLabel(YearMonth.of(2026, 12), januaryClock)
        )
    }

    @Test
    fun historyCountIsPluralized() {
        assertEquals("No payments", formatHistoryCount(0))
        assertEquals("1 payment", formatHistoryCount(1))
        assertEquals("3 payments", formatHistoryCount(3))
    }
}
