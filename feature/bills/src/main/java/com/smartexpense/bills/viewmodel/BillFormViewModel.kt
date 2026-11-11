package com.smartexpense.bills.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartexpense.bills.effect.BillFormEffect
import com.smartexpense.bills.intent.BillFormIntent
import com.smartexpense.bills.state.BillFormState
import com.smartexpense.bills.state.BillFormUiState
import com.smartexpense.bills.util.formatAmountForEditing
import com.smartexpense.core.common.util.MoneyInputUtil
import com.smartexpense.domain.model.Bill
import com.smartexpense.domain.model.BillAmountType
import com.smartexpense.domain.model.BillFrequency
import com.smartexpense.domain.usecase.AddBillUseCase
import com.smartexpense.domain.usecase.CategoryUseCase
import com.smartexpense.domain.usecase.DeleteBillUseCase
import com.smartexpense.domain.usecase.GetBillUseCase
import com.smartexpense.domain.usecase.SkipBillUseCase
import com.smartexpense.domain.usecase.UpdateBillUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BillFormViewModel(
    private val getBillUseCase: GetBillUseCase,
    private val addBillUseCase: AddBillUseCase,
    private val updateBillUseCase: UpdateBillUseCase,
    private val deleteBillUseCase: DeleteBillUseCase,
    private val skipBillUseCase: SkipBillUseCase,
    private val categoryUseCase: CategoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BillFormUiState())
    val uiState: StateFlow<BillFormUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<BillFormEffect>()
    val effect = _effect.asSharedFlow()

    private var loadedBillId: Long? = null
    private var hasLoaded = false

    init {
        viewModelScope.launch {
            categoryUseCase.getCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    /**
     * Initializes the form. A null [billId] starts a blank one-time bill; an id
     * loads the existing bill for editing.
     */
    fun load(billId: Long?) {
        // Keep in-progress edits when the screen is recreated (e.g. rotation).
        if (hasLoaded && loadedBillId == billId) return
        loadedBillId = billId
        hasLoaded = true

        if (billId == null) {
            _uiState.update {
                it.copy(
                    form = BillFormState(dueDateMillis = System.currentTimeMillis()),
                    isLoading = false
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val bill = getBillUseCase(billId)
                if (bill == null) {
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.emit(BillFormEffect.ShowError("Bill not found"))
                    return@launch
                }
                _uiState.update {
                    it.copy(form = bill.toFormState(), isLoading = false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                _effect.emit(
                    BillFormEffect.ShowError(e.message ?: "Could not load bill")
                )
            }
        }
    }

    fun onIntent(intent: BillFormIntent) {
        when (intent) {
            is BillFormIntent.NameChanged -> updateForm { it.copy(name = intent.value) }
            is BillFormIntent.AmountChanged -> updateForm { it.copy(amount = intent.value) }
            is BillFormIntent.DueDateChanged ->
                updateForm { it.copy(dueDateMillis = intent.millis) }
            is BillFormIntent.NotesChanged -> updateForm { it.copy(notes = intent.value) }
            is BillFormIntent.RecurringChanged ->
                updateForm { it.copy(isRecurring = intent.value) }
            is BillFormIntent.FrequencyChanged ->
                updateForm { it.copy(frequency = intent.value) }
            is BillFormIntent.IntervalChanged -> updateForm {
                it.copy(
                    intervalText = intent.value.filter { char -> char.isDigit() }.take(2)
                )
            }
            is BillFormIntent.AmountTypeChanged ->
                updateForm { it.copy(amountType = intent.value) }
            is BillFormIntent.CategoryChanged ->
                updateForm { it.copy(categoryId = intent.id) }
            is BillFormIntent.PayeeChanged -> updateForm { it.copy(payee = intent.value) }
            is BillFormIntent.PaymentMethodChanged ->
                updateForm { it.copy(paymentMethod = intent.value) }
            is BillFormIntent.AutopayChanged -> updateForm { it.copy(autopay = intent.value) }
            is BillFormIntent.ReminderDaysToggled -> updateForm {
                val current = it.reminderDays
                it.copy(
                    reminderDays = if (current.contains(intent.days)) {
                        current - intent.days
                    } else {
                        current + intent.days
                    }
                )
            }
            is BillFormIntent.ReminderTimeChanged -> updateForm {
                it.copy(reminderTimeMinutes = intent.minutes.coerceIn(0, 24 * 60 - 1))
            }
            is BillFormIntent.ActiveChanged -> updateForm { it.copy(isActive = intent.value) }

            BillFormIntent.Save -> save()
            BillFormIntent.Delete -> delete()
            BillFormIntent.Skip -> skip()
        }
    }

    private fun updateForm(transform: (BillFormState) -> BillFormState) {
        _uiState.update { it.copy(form = transform(it.form)) }
    }

    private fun save() {
        val state = _uiState.value.form
        if (state.isSaving) return

        if (state.name.isBlank()) {
            showError("Please enter a bill name")
            return
        }

        val amount = MoneyInputUtil.cleanAmount(state.amount).toDoubleOrNull()
        if (amount == null || amount <= 0) {
            showError("Please enter a valid amount")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(form = it.form.copy(isSaving = true)) }
            try {
                val billId = state.editingBillId
                if (billId == null) {
                    addBillUseCase(state.toBill(amount))
                } else {
                    val existing = getBillUseCase(billId)
                    if (existing == null) {
                        fail("Bill not found")
                        return@launch
                    }
                    updateBillUseCase(
                        state.toBill(amount).copy(
                            id = billId,
                            amountEstimated = if (existing.isPaid) {
                                existing.amountEstimated
                            } else {
                                state.amountType == BillAmountType.VARIABLE
                            },
                            paidAtMillis = existing.paidAtMillis,
                            skipped = existing.skipped
                        )
                    )
                }
                _effect.emit(BillFormEffect.Saved)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail(e.message ?: "Could not save bill")
            }
        }
    }

    private fun delete() {
        val billId = _uiState.value.form.editingBillId ?: return
        if (_uiState.value.form.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(form = it.form.copy(isSaving = true)) }
            try {
                deleteBillUseCase(billId)
                _effect.emit(BillFormEffect.Deleted)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail(e.message ?: "Could not delete bill")
            }
        }
    }

    private fun skip() {
        val state = _uiState.value.form
        val billId = state.editingBillId ?: return
        if (!state.canSkip || state.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(form = it.form.copy(isSaving = true)) }
            try {
                skipBillUseCase(billId)
                _effect.emit(BillFormEffect.Skipped)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail(e.message ?: "Could not skip bill")
            }
        }
    }

    private suspend fun fail(message: String) {
        _uiState.update { it.copy(form = it.form.copy(isSaving = false)) }
        _effect.emit(BillFormEffect.ShowError(message))
    }

    private fun showError(message: String) {
        viewModelScope.launch {
            _effect.emit(BillFormEffect.ShowError(message))
        }
    }

    private fun BillFormState.toBill(amount: Double): Bill = Bill(
        name = name.trim(),
        amount = amount,
        dueDateMillis = dueDateMillis,
        notes = notes,
        frequency = if (isRecurring) frequency else BillFrequency.NONE,
        repeatInterval = intervalText.toIntOrNull()?.coerceIn(1, 99) ?: 1,
        amountType = amountType,
        amountEstimated = amountType == BillAmountType.VARIABLE,
        categoryId = categoryId,
        payee = payee.trim(),
        paymentMethod = paymentMethod,
        autopay = autopay,
        reminderDaysBefore = reminderDays.maxOrNull() ?: 3,
        reminderTimeMinutes = reminderTimeMinutes,
        isActive = isActive
    )

    private fun Bill.toFormState(): BillFormState = BillFormState(
        name = name,
        amount = formatAmountForEditing(amount),
        dueDateMillis = dueDateMillis,
        notes = notes,
        isRecurring = isRecurring,
        frequency = if (isRecurring) frequency else BillFrequency.MONTHLY,
        intervalText = repeatInterval.coerceAtLeast(1).toString(),
        amountType = amountType,
        categoryId = categoryId,
        payee = payee,
        paymentMethod = paymentMethod,
        autopay = autopay,
        reminderDays = setOf(reminderDaysBefore.coerceAtLeast(0)),
        reminderTimeMinutes = reminderTimeMinutes,
        isActive = isActive,
        editingBillId = id,
        canSkip = !isPaid && !skipped
    )
}
