package com.smartexpense.paycheck.util

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One month of bill rows, shown under a section header. */
internal data class BillMonthSection<T>(
    val month: YearMonth,
    val label: String,
    val rows: List<T>,
    val total: Double
)

/**
 * Groups [rows] by the month of the due date each row displays. Rows arrive in
 * date order, so the sections come out in the same order and every bill stays
 * under the month it is paid for.
 */
internal fun <T> billMonthSections(
    rows: List<T>,
    zoneId: ZoneId,
    payDateMillis: Long,
    dueDateMillis: (T) -> Long,
    amount: (T) -> Double
): List<BillMonthSection<T>> {
    val payMonth = monthOf(payDateMillis, zoneId)
    return rows
        .groupBy { row -> monthOf(dueDateMillis(row), zoneId) }
        .map { (month, monthRows) ->
            BillMonthSection(
                month = month,
                label = monthHeader(month, payMonth),
                rows = monthRows,
                total = monthRows.sumOf(amount)
            )
        }
}

/** The calendar month [millis] falls in, using the app's zone. */
internal fun monthOf(millis: Long, zoneId: ZoneId): YearMonth =
    YearMonth.from(Instant.ofEpochMilli(millis).atZone(zoneId))

/**
 * Header for a month group. Bills due in the paycheck's own month read "Bills
 * for November 2026"; bills it pays ahead of time read "Bills covered for
 * December 2026".
 */
internal fun monthHeader(month: YearMonth, payMonth: YearMonth): String {
    val name = month.format(MONTH_FORMATTER)
    return if (month.isAfter(payMonth)) {
        "Bills covered for $name"
    } else {
        "Bills for $name"
    }
}

private val MONTH_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
