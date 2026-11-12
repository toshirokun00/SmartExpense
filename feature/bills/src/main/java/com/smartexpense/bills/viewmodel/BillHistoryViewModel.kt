package com.smartexpense.bills.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartexpense.bills.state.BillHistoryUiState
import com.smartexpense.bills.util.groupPaymentsByMonth
import com.smartexpense.domain.usecase.GetBillPaymentHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.YearMonth

/**
 * Collects every recorded bill payment for the signed-in user and groups it by
 * month. The [clock] is injected so month boundaries and the "this month" /
 * "last month" summaries are deterministic in tests.
 */
class BillHistoryViewModel(
    getBillPaymentHistoryUseCase: GetBillPaymentHistoryUseCase,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(BillHistoryUiState())
    val uiState: StateFlow<BillHistoryUiState> = _uiState

    init {
        viewModelScope.launch {
            getBillPaymentHistoryUseCase().collect { payments ->
                val months = groupPaymentsByMonth(payments, clock)
                val current = YearMonth.now(clock)
                val thisMonth = months.firstOrNull { it.yearMonth == current }
                val lastMonth = months.firstOrNull { it.yearMonth == current.minusMonths(1) }
                val selectedMonth = _uiState.value.selectedMonth
                    ?.takeIf { selected -> months.any { it.yearMonth == selected } }

                _uiState.update {
                    it.copy(
                        months = months,
                        selectedMonth = selectedMonth,
                        isLoading = false,
                        thisMonthTotal = thisMonth?.total ?: 0.0,
                        thisMonthCount = thisMonth?.count ?: 0,
                        lastMonthTotal = lastMonth?.total ?: 0.0,
                        lastMonthCount = lastMonth?.count ?: 0
                    )
                }
            }
        }
    }

    /** Filters the history to [month], or shows every month when `null`. */
    fun onMonthSelected(month: YearMonth?) {
        _uiState.update { it.copy(selectedMonth = month) }
    }
}
