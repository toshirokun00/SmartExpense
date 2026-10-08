package com.smartexpense.dashoard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartexpense.dashoard.effect.DashboardEffect
import com.smartexpense.dashoard.intent.DashboardIntent
import com.smartexpense.dashoard.state.DashboardUiState
import com.smartexpense.domain.usecase.GetExpenseUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(
    private val getExpenseUseCase: GetExpenseUseCase
) : ViewModel() {

    private val _effect = MutableSharedFlow<DashboardEffect>()
    val effect = _effect.asSharedFlow()

    private val expensesFlow = getExpenseUseCase()
        .catch { exception ->
            _effect.emit(
                DashboardEffect.ShowError(
                    exception.message ?: "Failed to load expenses"
                )
            )
        }

    val uiState: StateFlow<DashboardUiState> =
        expensesFlow
            .map { expenses ->
                DashboardUiState(
                    totalAmount = expenses.sumOf { it.amount },
                    expenseCount = expenses.size,
                    recentExpenses = expenses.take(5),
                    isLoading = false
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Companion.WhileSubscribed(5_000),
                initialValue = DashboardUiState(
                    isLoading = true
                )
            )

    fun onIntent(intent: DashboardIntent) {
        when (intent) {
            DashboardIntent.LoadDashboard -> {
                // The Flow is already observed by uiState.
            }

            DashboardIntent.RefreshDashboard -> TODO()
        }
    }
}