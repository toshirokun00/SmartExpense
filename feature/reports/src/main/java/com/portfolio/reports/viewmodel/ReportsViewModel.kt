package com.portfolio.reports.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.reports.effect.ReportsEffect
import com.portfolio.reports.effect.ReportsEffect.*
import com.portfolio.reports.intent.ReportsIntent
import com.portfolio.reports.state.ReportsUiState
import com.smartexpense.domain.usecase.CategoryUseCase
import com.smartexpense.domain.usecase.GetExpenseReportUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class ReportsViewModel(
    private val getExpenseReportUseCase: GetExpenseReportUseCase,
    private val categoryUseCase: CategoryUseCase
) : ViewModel() {

    private val zoneId = ZoneId.systemDefault()
    private val initialDateRange = getCurrentMonthRange()

    private val _uiState = MutableStateFlow(
        ReportsUiState(
            startDateInclusive = initialDateRange.first,
            endDateExclusive = initialDateRange.second,
            isLoading = true
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<ReportsEffect>()
    val effect = _effect.asSharedFlow()

    private var reportJob: Job? = null

    init {
        observeCategories()

        loadReport(
            startDateInclusive = initialDateRange.first,
            endDateExclusive = initialDateRange.second
        )
    }

    fun onIntent(intent: ReportsIntent) {
        when (intent) {
            is ReportsIntent.DateRangeChanged -> {
                if (intent.endDateExclusive <= intent.startDateInclusive) {
                    viewModelScope.launch {
                        _effect.emit(
                            ShowError(
                                "End date must be after start date"
                            )
                        )
                    }
                    return
                }

                loadReport(
                    startDateInclusive = intent.startDateInclusive,
                    endDateExclusive = intent.endDateExclusive
                )
            }

            ReportsIntent.Retry -> {
                val state = _uiState.value
                loadReport(
                    startDateInclusive = state.startDateInclusive,
                    endDateExclusive = state.endDateExclusive
                )
            }

            ReportsIntent.DatePickerDismissed -> {
                _uiState.update {
                    it.copy(
                        isStartDatePickerOpen = false,
                        isEndDatePickerOpen = false
                    )
                }

            }
            ReportsIntent.EndDatePickerOpened -> {
                _uiState.update {
                    it.copy(
                        isStartDatePickerOpen = false,
                        isEndDatePickerOpen = true
                    )
                }
            }
            ReportsIntent.StartDatePickerOpened -> {
                _uiState.update {
                    it.copy(
                        isStartDatePickerOpen = true,
                        isEndDatePickerOpen = false
                    )
                }
            }
        }
    }

    private fun observeCategories() {
        viewModelScope.launch {
            categoryUseCase.getCategories()
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            error = throwable.message
                                ?: "Failed to load categories",
                            isLoading = false
                        )
                    }

                    _effect.emit(
                        ReportsEffect.ShowError(
                            throwable.message ?: "Failed to load categories"
                        )
                    )
                }
                .collect { categories ->
                    _uiState.update { state ->
                        state.copy(
                            categoryNames = categories.associate {
                                it.id to it.name
                            }
                        )
                    }
                }
        }
    }

    private fun loadReport(
        startDateInclusive: Long,
        endDateExclusive: Long
    ) {
        reportJob?.cancel()

        _uiState.update {
            it.copy(
                startDateInclusive = startDateInclusive,
                endDateExclusive = endDateExclusive,
                isStartDatePickerOpen = false,
                isEndDatePickerOpen = false,
                isLoading = true,
                error = null
            )
        }

        reportJob = viewModelScope.launch {
            getExpenseReportUseCase(
                startDateInclusive = startDateInclusive,
                endDateExclusive = endDateExclusive
            )
                .catch { throwable ->
                    val message = throwable.message
                        ?: "Failed to load expense report"

                    _uiState.update {
                        it.copy(isLoading = false, error = message)
                    }

                    _effect.emit(ReportsEffect.ShowError(message))
                }
                .collect { report ->
                    _uiState.update {
                        it.copy(
                            report = report,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    private fun getCurrentMonthRange(): Pair<Long, Long> {
        val firstDay = LocalDate.now(zoneId).withDayOfMonth(1)
        val firstDayOfNextMonth = firstDay.plusMonths(1)

        return Pair(
            firstDay.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            firstDayOfNextMonth.atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()
        )
    }
}