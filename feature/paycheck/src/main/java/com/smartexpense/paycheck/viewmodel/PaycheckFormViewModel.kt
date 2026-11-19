package com.smartexpense.paycheck.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartexpense.core.common.util.MoneyInputUtil
import com.smartexpense.domain.model.Bill
import com.smartexpense.domain.model.BillPayment
import com.smartexpense.domain.model.Paycheck
import com.smartexpense.domain.model.PaycheckFrequency
import com.smartexpense.domain.model.PaycheckPlan
import com.smartexpense.domain.model.PaycheckSchedule
import com.smartexpense.domain.model.planPaychecks
import com.smartexpense.domain.usecase.AddPaycheckScheduleUseCase
import com.smartexpense.domain.usecase.AddPaycheckUseCase
import com.smartexpense.domain.usecase.DeletePaycheckUseCase
import com.smartexpense.domain.usecase.GetBillPaymentHistoryUseCase
import com.smartexpense.domain.usecase.GetBillsUseCase
import com.smartexpense.domain.usecase.GetPaycheckUseCase
import com.smartexpense.domain.usecase.GetPaychecksUseCase
import com.smartexpense.domain.usecase.StopPaycheckScheduleUseCase
import com.smartexpense.domain.usecase.UpdatePaycheckUseCase
import com.smartexpense.paycheck.effect.PaycheckFormEffect
import com.smartexpense.paycheck.intent.PaycheckFormIntent
import com.smartexpense.paycheck.state.PaycheckFormState
import com.smartexpense.paycheck.state.PaycheckFormUiState
import com.smartexpense.paycheck.util.formatAmountForEditing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class PaycheckFormViewModel(
    private val getPaycheckUseCase: GetPaycheckUseCase,
    private val addPaycheckUseCase: AddPaycheckUseCase,
    private val updatePaycheckUseCase: UpdatePaycheckUseCase,
    private val deletePaycheckUseCase: DeletePaycheckUseCase,
    private val addPaycheckScheduleUseCase: AddPaycheckScheduleUseCase,
    private val stopPaycheckScheduleUseCase: StopPaycheckScheduleUseCase,
    private val getBillsUseCase: GetBillsUseCase,
    private val getPaychecksUseCase: GetPaychecksUseCase,
    private val getBillPaymentHistoryUseCase: GetBillPaymentHistoryUseCase,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaycheckFormUiState(zoneId = clock.zone))
    val uiState: StateFlow<PaycheckFormUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<PaycheckFormEffect>()
    val effect = _effect.asSharedFlow()

    private var loadedPaycheckId: Long? = null
    private var hasLoaded = false
    private var userAdjustedBills = false
    private var appliedInitialSuggestions = false
    private var allPaychecks: List<Paycheck> = emptyList()

    init {
        viewModelScope.launch {
            combine(
                getBillsUseCase(),
                getPaychecksUseCase(),
                getBillPaymentHistoryUseCase()
            ) { bills, paychecks, payments ->
                Triple(bills.sortedBy { it.dueDateMillis }, paychecks, payments)
            }.collect { (sortedBills, paychecks, payments) ->
                allPaychecks = paychecks
                val state = _uiState.value
                val editingPaycheckId = state.form.editingPaycheckId
                val selection = selectionFor(
                    editingPaycheckId = editingPaycheckId,
                    payDateMillis = state.form.payDateMillis,
                    bills = sortedBills,
                    payments = payments
                )
                val coveredChanged = selection.coveredBillIds != state.coveredBillIds
                val shouldSuggest = sortedBills.isNotEmpty() &&
                    !userAdjustedBills &&
                    !state.isLoading &&
                    (!appliedInitialSuggestions || coveredChanged)
                val selected = if (shouldSuggest) {
                    selection.selectedBillIds
                } else {
                    state.form.selectedBillIds
                }
                _uiState.value = state.copy(
                    bills = sortedBills,
                    payments = payments,
                    coveredBillIds = selection.coveredBillIds,
                    dueByBillId = selection.dueByBillId,
                    cycleByBillId = selection.cycleByBillId,
                    form = state.form.copy(selectedBillIds = selected)
                )
                if (shouldSuggest) appliedInitialSuggestions = true
            }
        }
    }

    /**
     * The same projection the paycheck cards use, so the form and the cards
     * agree on which bills each cutoff covers.
     */
    private fun plansFor(
        paychecks: List<Paycheck> = allPaychecks,
        bills: List<Bill> = _uiState.value.bills,
        payments: List<BillPayment> = _uiState.value.payments
    ): Map<Long, PaycheckPlan> {
        val paymentsByBill = payments
            .groupBy { it.billId }
            .mapValues { (_, recorded) -> recorded.map { it.paidAtMillis } }
        return planPaychecks(
            paychecks = paychecks,
            bills = bills,
            zoneId = clock.zone,
            paymentsByBill = paymentsByBill
        )
    }

    /**
     * Bills hidden from the "Bills to cover" list. A one-time bill can only be
     * covered once, so it disappears once another paycheck claims it. Recurring
     * bills recur, so they stay listed on every cutoff.
     */
    private fun coveredOneTimeBillIds(
        plans: Map<Long, PaycheckPlan>,
        bills: List<Bill>,
        paycheckId: Long?
    ): Set<Long> {
        val oneTimeIds = bills.filter { !it.isRecurring }.map { it.id }.toSet()
        if (oneTimeIds.isEmpty()) return emptySet()
        return plans
            .filterKeys { it != paycheckId }
            .values
            .flatMap { it.billIds }
            .filter { it in oneTimeIds }
            .toSet()
    }

    /**
     * The plan for the paycheck being edited, or for a draft at [payDateMillis]
     * while a new one is being created, together with the bills the rest of the
     * schedule already covers.
     */
    private fun selectionFor(
        editingPaycheckId: Long?,
        payDateMillis: Long,
        bills: List<Bill>,
        payments: List<BillPayment>
    ): FormSelection {
        val paychecks = if (editingPaycheckId == null) {
            // A draft cutoff joins the schedule so its window ends at the next
            // pay date, exactly like the paycheck that will be saved.
            allPaychecks + draftPaycheck(payDateMillis)
        } else {
            paychecksAtDate(editingPaycheckId, payDateMillis)
        }
        val plans = plansFor(paychecks = paychecks, bills = bills, payments = payments)
        val planId = editingPaycheckId ?: DRAFT_PAYCHECK_ID
        val plan = plans[planId]

        // Cycles the cutoff would claim if it had no saved bills. They date the
        // rows the user unchecks, so the row keeps its month group instead of
        // falling back to the bill's own due date, and it still does so after a
        // save when the plan no longer covers it.
        val autoPlans = if (plan == null || plan.billIds.isEmpty()) {
            plans
        } else {
            plansFor(
                paychecks = paychecks.map { paycheck ->
                    if (paycheck.id == planId) {
                        paycheck.copy(billIds = emptyList())
                    } else {
                        paycheck
                    }
                },
                bills = bills,
                payments = payments
            )
        }
        val cycles = LinkedHashMap<Long, Long>()
        autoPlans[planId]?.dueByBillId?.let(cycles::putAll)
        // A saved cutoff may pin a cycle outside its own window (coverage moved
        // earlier), so the plan's pins win over the plain window projection.
        plan?.dueByBillId?.let(cycles::putAll)

        return FormSelection(
            selectedBillIds = plan?.billIds.orEmpty().toSet(),
            dueByBillId = plan?.dueByBillId.orEmpty(),
            cycleByBillId = cycles,
            coveredBillIds = coveredOneTimeBillIds(plans, bills, planId)
        )
    }

    private fun draftPaycheck(payDateMillis: Long): Paycheck = Paycheck(
        id = DRAFT_PAYCHECK_ID,
        amount = 0.0,
        payDateMillis = payDateMillis
    )

    private data class FormSelection(
        val selectedBillIds: Set<Long>,
        val dueByBillId: Map<Long, Long>,
        val cycleByBillId: Map<Long, Long>,
        val coveredBillIds: Set<Long>
    )

    /**
     * Initializes the form. A null [paycheckId] starts a new paycheck with the
     * bills due in its coverage window already selected.
     */
    fun load(paycheckId: Long?) {
        if (hasLoaded && loadedPaycheckId == paycheckId) return
        loadedPaycheckId = paycheckId
        hasLoaded = true
        userAdjustedBills = false
        appliedInitialSuggestions = false

        if (paycheckId == null) {
            val payDateMillis = clock.millis()
            val state = _uiState.value
            val selection = selectionFor(
                editingPaycheckId = null,
                payDateMillis = payDateMillis,
                bills = state.bills,
                payments = state.payments
            )
            _uiState.update {
                it.copy(
                    form = PaycheckFormState(
                        payDateMillis = payDateMillis,
                        selectedBillIds = selection.selectedBillIds
                    ),
                    coveredBillIds = selection.coveredBillIds,
                    dueByBillId = selection.dueByBillId,
                    cycleByBillId = selection.cycleByBillId,
                    isLoading = false
                )
            }
            applySuggestionsIfBillsReady()
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val paycheck = getPaycheckUseCase(paycheckId)
                if (paycheck == null) {
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.emit(PaycheckFormEffect.ShowError("Paycheck not found"))
                    return@launch
                }
                val hasSavedBills = paycheck.billIds.isNotEmpty()
                val bills = _uiState.value.bills
                val selection = selectionFor(
                    editingPaycheckId = paycheck.id,
                    payDateMillis = paycheck.payDateMillis,
                    bills = bills,
                    payments = _uiState.value.payments
                )
                val selected = if (hasSavedBills) {
                    paycheck.billIds.toSet()
                } else {
                    selection.selectedBillIds
                }
                _uiState.update {
                    it.copy(
                        form = PaycheckFormState(
                            amount = formatAmountForEditing(paycheck.amount),
                            payDateMillis = paycheck.payDateMillis,
                            label = paycheck.label,
                            selectedBillIds = selected,
                            editingPaycheckId = paycheck.id,
                            isRecurringOccurrence = paycheck.isRecurringOccurrence,
                            scheduleClientId = paycheck.scheduleClientId
                        ),
                        coveredBillIds = selection.coveredBillIds,
                        dueByBillId = selection.dueByBillId,
                        cycleByBillId = selection.cycleByBillId,
                        isLoading = false
                    )
                }
                // Saved assignments win; a generated paycheck with no
                // assignments shows the plan the paycheck card shows.
                userAdjustedBills = hasSavedBills
                appliedInitialSuggestions = hasSavedBills
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                _effect.emit(
                    PaycheckFormEffect.ShowError(
                        e.message ?: "Could not load paycheck"
                    )
                )
            }
        }
    }

    fun onIntent(intent: PaycheckFormIntent) {
        when (intent) {
            is PaycheckFormIntent.AmountChanged ->
                updateForm { it.copy(amount = intent.value) }

            is PaycheckFormIntent.PayDateChanged -> changePayDate(intent.millis)

            is PaycheckFormIntent.LabelChanged ->
                updateForm { it.copy(label = intent.value) }

            is PaycheckFormIntent.BillToggled -> toggleBill(intent.billId)

            is PaycheckFormIntent.RecurringChanged -> setRecurring(intent.enabled)

            is PaycheckFormIntent.FrequencyChanged -> setFrequency(intent.frequency)

            is PaycheckFormIntent.FirstDayChanged ->
                setDay(first = intent.day.coerceIn(1, 31))

            is PaycheckFormIntent.SecondDayChanged ->
                setDay(second = intent.day.coerceIn(1, 31))

            PaycheckFormIntent.StopRepeating -> stopRepeating()
            PaycheckFormIntent.Save -> save()
            PaycheckFormIntent.Delete -> delete()
        }
    }

    private fun changePayDate(millis: Long) {
        if (!_uiState.value.form.canEditPayDate) return

        val state = _uiState.value
        val selection = selectionFor(
            editingPaycheckId = state.form.editingPaycheckId,
            payDateMillis = millis,
            bills = state.bills,
            payments = state.payments
        )
        userAdjustedBills = false
        appliedInitialSuggestions = true
        _uiState.update {
            it.copy(
                coveredBillIds = selection.coveredBillIds,
                dueByBillId = selection.dueByBillId,
                cycleByBillId = selection.cycleByBillId,
                form = it.form.copy(
                    payDateMillis = millis,
                    selectedBillIds = selection.selectedBillIds
                )
            )
        }
    }

    /** The stored paychecks with the edited one moved to [payDateMillis]. */
    private fun paychecksAtDate(paycheckId: Long?, payDateMillis: Long): List<Paycheck> =
        if (paycheckId == null) {
            allPaychecks
        } else {
            allPaychecks.map { paycheck ->
                if (paycheck.id == paycheckId) {
                    paycheck.copy(payDateMillis = payDateMillis)
                } else {
                    paycheck
                }
            }
        }

    private fun setRecurring(enabled: Boolean) {
        val state = _uiState.value
        if (state.form.isEditing) return

        val selectedDay = Instant.ofEpochMilli(state.form.payDateMillis)
            .atZone(clock.zone)
            .toLocalDate()
            .dayOfMonth

        val updated = state.form.copy(
            isRecurring = enabled,
            firstDay = if (enabled) selectedDay else state.form.firstDay,
            secondDay = when {
                !enabled -> state.form.secondDay
                state.form.frequency != PaycheckFrequency.SEMI_MONTHLY ->
                    state.form.secondDay
                selectedDay <= 15 -> 30
                else -> 15
            }
        )

        // The chosen pay date is the schedule start, so it stays the first
        // generated paycheck. Changing the rule afterwards moves the preview.
        _uiState.value = state.copy(form = updated)
        if (enabled) reselectSuggestions()
    }

    private fun setFrequency(frequency: PaycheckFrequency) {
        val state = _uiState.value
        val updated = state.form.copy(frequency = frequency)
        val normalized = if (
            frequency == PaycheckFrequency.SEMI_MONTHLY &&
            updated.secondDay == updated.firstDay
        ) {
            updated.copy(secondDay = if (updated.firstDay <= 15) 30 else 15)
        } else {
            updated
        }

        _uiState.update {
            it.copy(
                form = normalized.copy(
                    payDateMillis = firstOccurrenceMillis(normalized)
                        ?: normalized.payDateMillis
                )
            )
        }
        reselectSuggestions()
    }

    private fun setDay(first: Int? = null, second: Int? = null) {
        val state = _uiState.value
        val updated = state.form.copy(
            firstDay = first ?: state.form.firstDay,
            secondDay = second ?: state.form.secondDay
        )
        _uiState.update {
            it.copy(
                form = updated.copy(
                    payDateMillis = firstOccurrenceMillis(updated)
                        ?: updated.payDateMillis
                )
            )
        }
        reselectSuggestions()
    }

    private fun firstOccurrenceMillis(form: PaycheckFormState): Long? {
        val today = LocalDate.now(clock)
        // Interval schedules step from the chosen pay date. Day-of-month
        // schedules start on the 1st of the current month, so a cutoff that
        // already passed this month (payday on the 15th, created on the 18th)
        // is still the schedule's first paycheck instead of being skipped
        // until next month.
        val isDayOfMonthRule = form.frequency == PaycheckFrequency.MONTHLY ||
            form.frequency == PaycheckFrequency.SEMI_MONTHLY
        val start = when (form.frequency) {
            PaycheckFrequency.WEEKLY,
            PaycheckFrequency.BIWEEKLY -> Instant
                .ofEpochMilli(form.payDateMillis)
                .atZone(clock.zone)
                .toLocalDate()

            PaycheckFrequency.MONTHLY,
            PaycheckFrequency.SEMI_MONTHLY -> today.withDayOfMonth(1)
        }
        // Day-of-month rules scan from the schedule start so the current
        // month is not dropped; interval rules still look forward from today
        // unless the user picked a future anchor.
        val occurrencesFrom = if (isDayOfMonthRule) start else maxOf(start, today)
        val preview = PaycheckSchedule(
            clientId = "preview",
            amount = 0.0,
            label = form.label,
            frequency = form.frequency,
            firstDay = form.firstDay,
            secondDay = form.secondDay,
            startDateMillis = start.atStartOfDay(clock.zone)
                .toInstant()
                .toEpochMilli()
        )
        return preview.occurrencesBetween(
            from = occurrencesFrom,
            to = today.plusYears(2),
            zoneId = clock.zone
        ).firstOrNull()
            ?.atStartOfDay(clock.zone)
            ?.toInstant()
            ?.toEpochMilli()
    }

    private fun reselectSuggestions() {
        val state = _uiState.value
        val selection = selectionFor(
            editingPaycheckId = state.form.editingPaycheckId,
            payDateMillis = state.form.payDateMillis,
            bills = state.bills,
            payments = state.payments
        )
        userAdjustedBills = false
        appliedInitialSuggestions = true
        _uiState.update {
            it.copy(
                coveredBillIds = selection.coveredBillIds,
                dueByBillId = selection.dueByBillId,
                cycleByBillId = selection.cycleByBillId,
                form = it.form.copy(selectedBillIds = selection.selectedBillIds)
            )
        }
    }

    private fun toggleBill(billId: Long) {
        userAdjustedBills = true
        appliedInitialSuggestions = true
        updateForm { form ->
            val selected = form.selectedBillIds
            form.copy(
                selectedBillIds = if (billId in selected) {
                    selected - billId
                } else {
                    selected + billId
                }
            )
        }
    }

    private fun save() {
        val state = _uiState.value
        val form = state.form
        if (form.isSaving) return

        val amount = MoneyInputUtil.cleanAmount(form.amount).toDoubleOrNull()
        if (amount == null || amount <= 0) {
            showError("Please enter a valid paycheck amount")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(form = it.form.copy(isSaving = true)) }
            try {
                if (form.isEditing) {
                    updatePaycheckUseCase(
                        Paycheck(
                            id = form.editingPaycheckId ?: 0L,
                            amount = amount,
                            payDateMillis = form.payDateMillis,
                            label = form.label.trim(),
                            billIds = form.selectedBillIds.toList()
                        )
                    )
                } else if (form.isRecurring) {
                    addPaycheckScheduleUseCase(
                        schedule = PaycheckSchedule(
                            clientId = UUID.randomUUID().toString(),
                            amount = amount,
                            label = form.label.trim(),
                            frequency = form.frequency,
                            firstDay = form.firstDay,
                            secondDay = if (
                                form.frequency == PaycheckFrequency.SEMI_MONTHLY
                            ) {
                                form.secondDay
                            } else {
                                null
                            },
                            startDateMillis = form.payDateMillis,
                            createdAt = clock.millis()
                        ),
                        // The selection applies to the first cutoff only. Later
                        // cutoffs start empty so no bill is covered twice.
                        billIds = form.selectedBillIds.toList()
                    )
                } else {
                    addPaycheckUseCase(
                        Paycheck(
                            amount = amount,
                            payDateMillis = form.payDateMillis,
                            label = form.label.trim(),
                            billIds = form.selectedBillIds.toList(),
                            createdAt = clock.millis()
                        )
                    )
                }
                _effect.emit(PaycheckFormEffect.Saved)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(form = it.form.copy(isSaving = false)) }
                _effect.emit(
                    PaycheckFormEffect.ShowError(
                        e.message ?: "Could not save paycheck"
                    )
                )
            }
        }
    }

    private fun delete() {
        val paycheckId = _uiState.value.form.editingPaycheckId ?: return
        if (_uiState.value.form.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(form = it.form.copy(isSaving = true)) }
            try {
                deletePaycheckUseCase(paycheckId)
                _effect.emit(PaycheckFormEffect.Deleted)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(form = it.form.copy(isSaving = false)) }
                _effect.emit(
                    PaycheckFormEffect.ShowError(
                        e.message ?: "Could not delete paycheck"
                    )
                )
            }
        }
    }

    private fun stopRepeating() {
        val form = _uiState.value.form
        val clientId = form.scheduleClientId ?: return
        if (form.isSaving) return

        viewModelScope.launch {
            try {
                stopPaycheckScheduleUseCase(
                    clientId,
                    form.payDateMillis
                )
                _uiState.update {
                    it.copy(form = it.form.copy(scheduleStopped = true))
                }
                _effect.emit(PaycheckFormEffect.RecurringStopped)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.emit(
                    PaycheckFormEffect.ShowError(
                        e.message ?: "Could not stop recurring paychecks"
                    )
                )
            }
        }
    }

    private fun applySuggestionsIfBillsReady() {
        val state = _uiState.value
        if (state.bills.isEmpty()) return
        if (userAdjustedBills || appliedInitialSuggestions) return

        val selection = selectionFor(
            editingPaycheckId = state.form.editingPaycheckId,
            payDateMillis = state.form.payDateMillis,
            bills = state.bills,
            payments = state.payments
        )
        _uiState.update {
            it.copy(
                coveredBillIds = selection.coveredBillIds,
                dueByBillId = selection.dueByBillId,
                cycleByBillId = selection.cycleByBillId,
                form = it.form.copy(selectedBillIds = selection.selectedBillIds)
            )
        }
        appliedInitialSuggestions = true
    }

    private fun updateForm(transform: (PaycheckFormState) -> PaycheckFormState) {
        _uiState.update { it.copy(form = transform(it.form)) }
    }

    private fun showError(message: String) {
        viewModelScope.launch {
            _effect.emit(PaycheckFormEffect.ShowError(message))
        }
    }
}

/**
 * Id of the paycheck being created. It joins the schedule while the form is
 * open so its coverage window ends at the next pay date, but it is never saved
 * with this id.
 */
private const val DRAFT_PAYCHECK_ID = -1L
