package com.smartexpense.paycheck.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartexpense.domain.model.Bill
import com.smartexpense.domain.model.BillPayment
import com.smartexpense.domain.model.Paycheck
import com.smartexpense.domain.model.coveredOccurrenceDueMillis
import com.smartexpense.domain.model.isOccurrencePaid
import com.smartexpense.domain.model.planPaychecks
import com.smartexpense.domain.usecase.EnsurePaycheckOccurrencesUseCase
import com.smartexpense.domain.usecase.GetBillPaymentHistoryUseCase
import com.smartexpense.domain.usecase.GetBillsUseCase
import com.smartexpense.domain.usecase.GetPaychecksUseCase
import com.smartexpense.paycheck.intent.PaychecksIntent
import com.smartexpense.paycheck.state.PaychecksUiState
import com.smartexpense.paycheck.state.PlannedBill
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.YearMonth

class PaychecksViewModel(
    private val getPaychecksUseCase: GetPaychecksUseCase,
    private val getBillsUseCase: GetBillsUseCase,
    private val getBillPaymentHistoryUseCase: GetBillPaymentHistoryUseCase,
    private val ensurePaycheckOccurrencesUseCase: EnsurePaycheckOccurrencesUseCase,
    private val clock: Clock
) : ViewModel() {

    private val currentMonth = YearMonth.now(clock)
    private var allPaychecks: List<Paycheck> = emptyList()
    private var billPayments: List<BillPayment> = emptyList()

    private val _uiState = MutableStateFlow(
        PaychecksUiState(
            selectedMonth = currentMonth,
            currentMonth = currentMonth,
            zoneId = clock.zone,
            isLoading = true
        )
    )
    val uiState: StateFlow<PaychecksUiState> = _uiState.asStateFlow()

    init {
        ensureMonthIsMaterialized(currentMonth)
        viewModelScope.launch {
            getPaychecksUseCase().collect { paychecks ->
                allPaychecks = paychecks
                recompute(isLoading = false)
            }
        }
        viewModelScope.launch {
            combine(
                getBillsUseCase(),
                getBillPaymentHistoryUseCase()
            ) { bills, payments ->
                bills to payments
            }.collect { (bills, payments) ->
                billPayments = payments
                _uiState.update { it.copy(bills = bills) }
                recompute()
            }
        }
    }

    fun onIntent(intent: PaychecksIntent) {
        when (intent) {
            PaychecksIntent.PreviousMonth ->
                showMonth(_uiState.value.selectedMonth.minusMonths(1))

            PaychecksIntent.NextMonth ->
                showMonth(_uiState.value.selectedMonth.plusMonths(1))

            PaychecksIntent.CurrentMonth -> showMonth(currentMonth)

            is PaychecksIntent.SelectMonth -> showMonth(
                YearMonth.of(intent.year, intent.month)
            )
        }
    }

    private fun showMonth(month: YearMonth) {
        _uiState.update { it.copy(selectedMonth = month) }
        recompute()
        ensureMonthIsMaterialized(month)
    }

    /**
     * Creates the recurring paychecks of [month] if the schedule has not
     * materialized them yet, so a monthly payroll shows a cutoff for every
     * month the user browses, including months outside the rolling window.
     */
    private fun ensureMonthIsMaterialized(month: YearMonth) {
        viewModelScope.launch {
            try {
                ensurePaycheckOccurrencesUseCase(
                    month.atDay(1),
                    month.atEndOfMonth()
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                // Failing to generate leaves the stored paychecks on screen;
                // the month must still render.
            }
        }
    }

    private fun recompute(isLoading: Boolean? = null) {
        _uiState.update { state ->
            val inMonth = paychecksInMonth(allPaychecks, state.selectedMonth)
            state.copy(
                paychecks = inMonth,
                plannedBills = plannedBillsFor(
                    paychecks = allPaychecks,
                    bills = state.bills
                ),
                isLoading = isLoading ?: state.isLoading
            )
        }
    }

    private fun paychecksInMonth(
        paychecks: List<Paycheck>,
        month: YearMonth
    ): List<Paycheck> = paychecks.filter { paycheck ->
        YearMonth.from(paycheck.payDate(clock.zone)) == month
    }

    /**
     * The bills every paycheck covers, keyed by paycheck id and pinned to the
     * cycle each paycheck claimed. A recurring bill's row advances its due date
     * when a cycle is paid, so the covered cycle is resolved from the pay date
     * instead of the bill's current stored date.
     *
     * Rows are ordered by the covered cycle's due date so the card reads like a
     * payment calendar, independent of the saved assignment order.
     */
    private fun plannedBillsFor(
        paychecks: List<Paycheck>,
        bills: List<Bill>
    ): Map<Long, List<PlannedBill>> {
        val billsById = bills.associateBy { it.id }
        val paymentsByBill = billPayments
            .groupBy { it.billId }
            .mapValues { (_, payments) -> payments.map { it.paidAtMillis } }
        val plans = planPaychecks(
            paychecks = paychecks,
            bills = bills,
            zoneId = clock.zone,
            paymentsByBill = paymentsByBill
        )
        return paychecks.associate { paycheck ->
            val plan = plans[paycheck.id]
            val covered = plan?.billIds.orEmpty()
                .mapNotNull { billId ->
                    val bill = billsById[billId] ?: return@mapNotNull null
                    val dueDateMillis = plan?.dueByBillId?.get(billId)
                        ?: coveredOccurrenceDueMillis(
                            bill = bill,
                            payDateMillis = paycheck.payDateMillis,
                            paidAtMillis = paymentsByBill[billId].orEmpty(),
                            zoneId = clock.zone
                        )
                    PlannedBill(
                        billId = billId,
                        dueDateMillis = dueDateMillis,
                        isPaid = isOccurrencePaid(bill, dueDateMillis, clock.zone)
                    )
                }
                .sortedWith(
                    compareBy(
                        { planned -> planned.dueDateMillis },
                        { planned -> billsById[planned.billId]?.name.orEmpty() },
                        { planned -> planned.billId }
                    )
                )
            paycheck.id to covered
        }
    }

}
