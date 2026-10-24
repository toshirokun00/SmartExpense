package com.smartexpense.recurring.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartexpense.domain.model.RecurrenceFrequency
import com.smartexpense.domain.model.RecurringExpenseSchedule
import com.smartexpense.domain.usecase.CategoryUseCase
import com.smartexpense.domain.usecase.ProcessRecurringExpensesUseCase
import com.smartexpense.domain.usecase.RecurringExpenseUseCase
import com.smartexpense.recurring.effect.RecurringExpenseEffect
import com.smartexpense.recurring.intent.RecurringExpenseIntent
import com.smartexpense.recurring.state.RecurringExpenseScreenState
import com.smartexpense.recurring.state.RecurringExpenseSummaryState
import com.smartexpense.recurring.state.RecurringExpenseUiState
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

class RecurringExpenseListViewModel(
    private val recurringExpenseUseCase: RecurringExpenseUseCase,
    private val categoryUseCase: CategoryUseCase,
    private val processRecurringExpensesUseCase: ProcessRecurringExpensesUseCase
) : ViewModel() {

    private val _screenState = MutableStateFlow(
        RecurringExpenseScreenState()
    )

    val screenState: StateFlow<RecurringExpenseScreenState> =
        _screenState.asStateFlow()

    private val _effect = MutableSharedFlow<RecurringExpenseEffect>()

    val effect = _effect.asSharedFlow()

    val uiState: StateFlow<RecurringExpenseUiState> =
        combine(
            recurringExpenseUseCase.getSchedules(),
            categoryUseCase.getCategories()
        ) { schedules, categories ->
            RecurringExpenseUiState(
                isLoading = false,
                schedules = schedules.map { schedule ->
                    RecurringExpenseSummaryState(
                        schedule = schedule,
                        categoryName = categories
                            .firstOrNull { it.id == schedule.categoryId }
                            ?.name
                            ?: "Unknown",
                        frequencyLabel = schedule.frequencyLabel(),
                        status = schedule.status()
                    )
                }
            )
        }
            .catch { exception ->
                emit(
                    RecurringExpenseUiState(
                        isLoading = false,
                        errorMessage = exception.message
                            ?: "Failed to load recurring expenses"
                    )
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = RecurringExpenseUiState(isLoading = true)
            )

    init {
        processDueSchedules()
    }

    fun onIntent(intent: RecurringExpenseIntent) {
        when (intent) {
            is RecurringExpenseIntent.Pause ->
                updateActiveState(
                    id = intent.id,
                    isActive = false
                )

            is RecurringExpenseIntent.Resume ->
                updateActiveState(
                    id = intent.id,
                    isActive = true
                )

            is RecurringExpenseIntent.RequestDelete ->
                requestDelete(intent.id)

            RecurringExpenseIntent.ConfirmDelete ->
                confirmDelete()

            RecurringExpenseIntent.CancelDelete ->
                cancelDelete()
        }
    }

    private fun processDueSchedules() {
        viewModelScope.launch {
            try {
                processRecurringExpensesUseCase()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _effect.emit(
                    RecurringExpenseEffect.ShowError(
                        exception.message
                            ?: "Failed to process recurring expenses"
                    )
                )
            }
        }
    }

    private fun updateActiveState(
        id: Long,
        isActive: Boolean
    ) {
        viewModelScope.launch {
            try {
                if (isActive) {
                    recurringExpenseUseCase.resumeSchedule(id)
                    _effect.emit(RecurringExpenseEffect.ScheduleResumed)
                } else {
                    recurringExpenseUseCase.pauseSchedule(id)
                    _effect.emit(RecurringExpenseEffect.SchedulePaused)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _effect.emit(
                    RecurringExpenseEffect.ShowError(
                        exception.message
                            ?: "Failed to update recurring expense"
                    )
                )
            }
        }
    }

    private fun requestDelete(id: Long) {
        _screenState.update {
            it.copy(
                showDeleteConfirmation = true,
                scheduleToDeleteId = id
            )
        }
    }

    private fun confirmDelete() {
        val scheduleId = _screenState.value.scheduleToDeleteId
            ?: return

        viewModelScope.launch {
            try {
                recurringExpenseUseCase.deleteSchedule(scheduleId)

                _screenState.update {
                    it.copy(
                        showDeleteConfirmation = false,
                        scheduleToDeleteId = null
                    )
                }

                _effect.emit(RecurringExpenseEffect.ScheduleDeleted)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _effect.emit(
                    RecurringExpenseEffect.ShowError(
                        exception.message
                            ?: "Failed to delete recurring expense"
                    )
                )
            }
        }
    }

    private fun cancelDelete() {
        _screenState.update {
            it.copy(
                showDeleteConfirmation = false,
                scheduleToDeleteId = null
            )
        }
    }
}

private fun RecurringExpenseSchedule.frequencyLabel(): String {
    return when (frequency) {
        RecurrenceFrequency.WEEKLY ->
            if (interval == 1) "Weekly" else "Every $interval weeks"

        RecurrenceFrequency.MONTHLY ->
            if (interval == 1) "Monthly" else "Every $interval months"

        RecurrenceFrequency.YEARLY ->
            if (interval == 1) "Yearly" else "Every $interval years"
    }
}
