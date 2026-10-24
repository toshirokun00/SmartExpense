package com.smartexpense.recurring.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartexpense.domain.model.RecurringExpenseSchedule
import com.smartexpense.domain.recurring.RecurringDateUtils
import com.smartexpense.domain.recurring.RecurringScheduleValidator
import com.smartexpense.domain.usecase.CategoryUseCase
import com.smartexpense.domain.usecase.RecurringExpenseUseCase
import com.smartexpense.recurring.effect.RecurringExpenseFormEffect
import com.smartexpense.recurring.intent.RecurringExpenseFormIntent
import com.smartexpense.recurring.state.RecurringExpenseFormState
import com.smartexpense.recurring.state.RecurringExpenseFormUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

class RecurringExpenseFormViewModel(
    private val recurringExpenseUseCase: RecurringExpenseUseCase,
    private val categoryUseCase: CategoryUseCase,
    clock: Clock
) : ViewModel() {

    private val _formState = MutableStateFlow(
        RecurringExpenseFormState(
            startDate = RecurringDateUtils.toEpochMillis(
                date = LocalDate.now(clock),
                zoneId = clock.zone
            )
        )
    )

    val formState: StateFlow<RecurringExpenseFormState> =
        _formState.asStateFlow()

    private val _effect = MutableSharedFlow<RecurringExpenseFormEffect>()

    val effect = _effect.asSharedFlow()

    private val categoriesFlow = categoryUseCase.getCategories()
        .catch { exception ->
            _effect.emit(
                RecurringExpenseFormEffect.ShowError(
                    exception.message
                        ?: "Failed to load categories"
                )
            )
            emit(emptyList())
        }

    val uiState: StateFlow<RecurringExpenseFormUiState> =
        combine(
            categoriesFlow,
            _formState
        ) { categories, form ->
            RecurringExpenseFormUiState(
                form = form,
                categories = categories
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = RecurringExpenseFormUiState(
                    form = _formState.value
                )
            )

    fun onIntent(intent: RecurringExpenseFormIntent) {
        when (intent) {
            is RecurringExpenseFormIntent.DescriptionChanged ->
                _formState.update {
                    it.copy(description = intent.description)
                }

            is RecurringExpenseFormIntent.AmountChanged ->
                _formState.update {
                    it.copy(amount = intent.amount)
                }

            is RecurringExpenseFormIntent.CategoryChanged ->
                _formState.update {
                    it.copy(categoryId = intent.categoryId)
                }

            is RecurringExpenseFormIntent.FrequencyChanged ->
                _formState.update {
                    it.copy(frequency = intent.frequency)
                }

            is RecurringExpenseFormIntent.IntervalChanged ->
                _formState.update {
                    it.copy(interval = intent.interval)
                }

            is RecurringExpenseFormIntent.StartDateChanged ->
                _formState.update {
                    it.copy(startDate = intent.date)
                }

            is RecurringExpenseFormIntent.EndDateChanged ->
                _formState.update {
                    it.copy(endDate = intent.date)
                }

            is RecurringExpenseFormIntent.LoadSchedule ->
                loadSchedule(intent.id)

            RecurringExpenseFormIntent.Save ->
                save()
        }
    }

    private fun loadSchedule(id: Long) {
        val state = _formState.value

        if (state.editingScheduleId == id && !state.isLoading) {
            return
        }

        _formState.update {
            it.copy(
                editingScheduleId = id,
                isLoading = true
            )
        }

        viewModelScope.launch {
            try {
                val schedule = recurringExpenseUseCase.getScheduleById(id)

                if (schedule == null) {
                    _formState.update {
                        it.copy(isLoading = false)
                    }
                    _effect.emit(
                        RecurringExpenseFormEffect.ShowError(
                            "Recurring expense not found"
                        )
                    )
                    return@launch
                }

                _formState.update {
                    it.copy(
                        editingScheduleId = schedule.id,
                        clientId = schedule.clientId,
                        description = schedule.description,
                        amount = schedule.amount.toString(),
                        categoryId = schedule.categoryId,
                        frequency = schedule.frequency,
                        interval = schedule.interval.toString(),
                        startDate = schedule.startDate,
                        endDate = schedule.endDate,
                        isLoading = false
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _formState.update {
                    it.copy(isLoading = false)
                }
                _effect.emit(
                    RecurringExpenseFormEffect.ShowError(
                        exception.message
                            ?: "Failed to load recurring expense"
                    )
                )
            }
        }
    }

    private fun save() {
        val state = _formState.value

        if (state.isSaving || state.isLoading) {
            return
        }

        val amount = state.amount.trim().toDoubleOrNull()

        if (amount == null) {
            showError("Please enter a valid amount")
            return
        }

        val interval = state.interval.trim().toIntOrNull()

        if (interval == null) {
            showError("Please enter a valid repeat interval")
            return
        }

        val categoryId = state.categoryId

        if (categoryId == null) {
            showError("Please select a category")
            return
        }

        val startDate = state.startDate

        if (startDate == null) {
            showError("Please select a start date")
            return
        }

        val schedule = RecurringExpenseSchedule(
            id = state.editingScheduleId ?: 0L,
            clientId = state.clientId,
            description = state.description.trim(),
            amount = amount,
            categoryId = categoryId,
            frequency = state.frequency,
            interval = interval,
            startDate = startDate,
            endDate = state.endDate,
            nextOccurrence = startDate,
            isActive = true
        )

        RecurringScheduleValidator.validate(schedule)?.let { message ->
            showError(message)
            return
        }

        // Disable the button before suspending so a double tap cannot submit
        // twice. On success this stays disabled until the screen navigates.
        _formState.update {
            it.copy(isSaving = true)
        }

        viewModelScope.launch {
            try {
                if (state.editingScheduleId == null) {
                    recurringExpenseUseCase.addSchedule(schedule)
                } else {
                    recurringExpenseUseCase.updateSchedule(schedule)
                }

                _effect.emit(RecurringExpenseFormEffect.Saved)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _formState.update {
                    it.copy(isSaving = false)
                }
                _effect.emit(
                    RecurringExpenseFormEffect.ShowError(
                        exception.message
                            ?: "Failed to save recurring expense"
                    )
                )
            }
        }
    }

    private fun showError(message: String) {
        viewModelScope.launch {
            _effect.emit(
                RecurringExpenseFormEffect.ShowError(message)
            )
        }
    }
}
