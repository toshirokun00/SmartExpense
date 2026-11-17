package com.smartexpense.bills.util

import com.smartexpense.domain.model.Bill
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MONTH_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

/**
 * One calendar month of bills, shown under a month header on the bills list.
 * Bills are sorted by due date so the earliest payment comes first.
 */
internal data class BillMonthGroup(
    val yearMonth: YearMonth,
    val label: String,
    val bills: List<Bill>,
    val total: Double
) {
    val count: Int get() = bills.size
}

/**
 * Splits [bills] into calendar months using the due date in [clock]'s zone,
 * oldest month first. Every bill lands in exactly one group, so the list shows
 * the current month, the next month, and any earlier or later cycle without
 * dropping rows.
 */
internal fun groupBillsByMonth(
    bills: List<Bill>,
    clock: Clock = Clock.systemDefaultZone()
): List<BillMonthGroup> {
    val zoneId = clock.zone
    val currentMonth = YearMonth.now(clock)
    return bills
        .sortedBy { it.dueDateMillis }
        .groupBy { bill ->
            YearMonth.from(
                Instant.ofEpochMilli(bill.dueDateMillis).atZone(zoneId)
            )
        }
        .map { (month, monthBills) ->
            BillMonthGroup(
                yearMonth = month,
                label = billMonthLabel(month, currentMonth),
                bills = monthBills,
                total = monthBills.sumOf { it.amount }
            )
        }
}

/**
 * Header label for a month group. The current and next month are named
 * relative to today, matching the bill history screen, so the usual
 * current/next pair is obvious at a glance.
 */
internal fun billMonthLabel(month: YearMonth, currentMonth: YearMonth): String {
    val formatted = month.format(MONTH_FORMATTER)
    return when (month) {
        currentMonth -> "This month • $formatted"
        currentMonth.plusMonths(1) -> "Next month • $formatted"
        else -> formatted
    }
}
