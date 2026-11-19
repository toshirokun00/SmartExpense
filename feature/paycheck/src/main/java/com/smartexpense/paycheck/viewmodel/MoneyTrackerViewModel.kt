package com.smartexpense.paycheck.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartexpense.domain.model.buildPaycheckEnvelopes
import com.smartexpense.domain.usecase.GetBillsUseCase
import com.smartexpense.domain.usecase.GetExpenseUseCase
import com.smartexpense.domain.usecase.GetPaychecksUseCase
import com.smartexpense.paycheck.state.MoneyTrackerUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock

class MoneyTrackerViewModel(
    private val getPaychecksUseCase: GetPaychecksUseCase,
    private val getBillsUseCase: GetBillsUseCase,
    private val getExpenseUseCase: GetExpenseUseCase,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoneyTrackerUiState())
    val uiState: StateFlow<MoneyTrackerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                getPaychecksUseCase(),
                getBillsUseCase(),
                getExpenseUseCase()
            ) { paychecks, bills, expenses ->
                buildPaycheckEnvelopes(
                    paychecks = paychecks,
                    bills = bills,
                    expenses = expenses,
                    nowMillis = clock.millis(),
                    zoneId = clock.zone
                )
            }
                .catch {
                    _uiState.update { it.copy(isLoading = false) }
                }
                .collect { envelopes ->
                    _uiState.update {
                        it.copy(envelopes = envelopes, isLoading = false)
                    }
                }
        }
    }
}
