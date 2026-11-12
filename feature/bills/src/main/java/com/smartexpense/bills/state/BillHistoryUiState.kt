package com.smartexpense.bills.state

import com.smartexpense.domain.model.BillPayment
import java.time.YearMonth

/**
 * A single calendar month of recorded bill payments, newest month first.
 */
data class BillPaymentMonth(
    val yearMonth: YearMonth,
    val label: String,
    val shortLabel: String,
    val payments: List<BillPayment>
) {
    val count: Int get() = payments.size

    val total: Double get() = payments.sumOf { it.amount }
}

/**
 * State for the paid-bill history screen. Totals for the current and previous
 * month are pre-computed so the summaries stay consistent with the groups.
 *
 * [selectedMonth] filters the visible groups; `null` shows every month.
 */
data class BillHistoryUiState(
    val months: List<BillPaymentMonth> = emptyList(),
    val selectedMonth: YearMonth? = null,
    val isLoading: Boolean = true,
    val thisMonthTotal: Double = 0.0,
    val thisMonthCount: Int = 0,
    val lastMonthTotal: Double = 0.0,
    val lastMonthCount: Int = 0
) {
    val isEmpty: Boolean get() = !isLoading && months.isEmpty()

    /** Months to render, respecting the month filter. */
    val visibleMonths: List<BillPaymentMonth>
        get() = selectedMonth?.let { selected ->
            months.filter { it.yearMonth == selected }
        } ?: months

    /** Label for the filter field: the chosen month, or "All months". */
    val selectedMonthLabel: String
        get() = selectedMonth?.let { selected ->
            months.firstOrNull { it.yearMonth == selected }?.shortLabel
                ?: selected.toString()
        } ?: ALL_MONTHS_LABEL

    companion object {
        const val ALL_MONTHS_LABEL: String = "All months"
    }
}
