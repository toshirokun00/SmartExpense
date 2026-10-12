package com.portfolio.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val getExpenseReportUseCase: GetExpenseReportUseCase
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
                            ReportsEffect.ShowError(
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
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message
                                ?: "Failed to load expense report"
                        )
                    }

                    _effect.emit(
                        ReportsEffect.ShowError(
                            throwable.message
                                ?: "Failed to load expense report"
                        )
                    )
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
        val today = LocalDate.now(zoneId)
        val startOfMonth = today.withDayOfMonth(1)
        val startOfNextMonth = startOfMonth.plusMonths(1)

        return Pair(
            startOfMonth.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            startOfNextMonth.atStartOfDay(zoneId).toInstant().toEpochMilli()
        )
    }
}