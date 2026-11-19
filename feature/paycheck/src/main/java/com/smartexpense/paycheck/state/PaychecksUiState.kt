package com.smartexpense.paycheck.state

import com.smartexpense.domain.model.Bill
import com.smartexpense.domain.model.Paycheck
import java.time.YearMonth
import java.time.ZoneId

/**
 * A bill a paycheck covers, pinned to the cycle that paycheck claimed. The due
 * date stays fixed even after the bill is paid and its row advances to the
 * next cycle, so the paycheck keeps showing what it covered.
 */
data class PlannedBill(
    val billId: Long,
    val dueDateMillis: Long,
    val isPaid: Boolean
)

data class PaychecksUiState(
    /** Paychecks whose pay date falls in [selectedMonth]. */
    val paychecks: List<Paycheck> = emptyList(),
    /**
     * The bills each paycheck covers, keyed by paycheck id, pinned to the
     * cycle that paycheck covered. Saved assignments win; a cutoff with
     * nothing saved shows the bills the form would suggest, so upcoming
     * months display their planned coverage too.
     */
    val plannedBills: Map<Long, List<PlannedBill>> = emptyMap(),
    val bills: List<Bill> = emptyList(),
    val selectedMonth: YearMonth = YearMonth.now(),
    val currentMonth: YearMonth = YearMonth.now(),
    /** Zone used to resolve the calendar months of the covered cycles. */
    val zoneId: ZoneId = ZoneId.systemDefault(),
    val isLoading: Boolean = true
)
