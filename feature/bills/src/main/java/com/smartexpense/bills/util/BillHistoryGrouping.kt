package com.smartexpense.bills.util

import com.smartexpense.bills.state.BillPaymentMonth
import com.smartexpense.domain.model.BillPayment
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MONTH_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

/**
 * Groups [payments] into calendar months using [clock]'s time zone, newest
 * month first and newest payment first inside each month. Payments are never
 * dropped: every record belongs to exactly one month.
 */
internal fun groupPaymentsByMonth(
    payments: List<BillPayment>,
    clock: Clock
): List<BillPaymentMonth> =
    payments
        .groupBy { payment ->
            Instant.ofEpochMilli(payment.paidAtMillis)
                .atZone(clock.zone)
                .toLocalDate()
                .let(YearMonth::from)
        }
        .toSortedMap(compareByDescending { it })
        .map { (yearMonth, monthPayments) ->
            BillPaymentMonth(
                yearMonth = yearMonth,
                label = monthLabel(yearMonth, clock),
                shortLabel = yearMonth.format(MONTH_FORMATTER),
                payments = monthPayments.sortedByDescending { it.paidAtMillis }
            )
        }

/** "1 payment" / "N payments" label used on the summary cards. */
internal fun formatHistoryCount(count: Int): String = when (count) {
    0 -> "No payments"
    1 -> "1 payment"
    else -> "$count payments"
}

/**
 * Human label for a month group. The current and previous month are named
 * relative to today so "this month or previous" is obvious at a glance.
 */
internal fun monthLabel(yearMonth: YearMonth, clock: Clock): String {
    val current = YearMonth.now(clock)
    val formatted = yearMonth.format(MONTH_FORMATTER)
    return when (yearMonth) {
        current -> "This month • $formatted"
        current.minusMonths(1) -> "Last month • $formatted"
        else -> formatted
    }
}
