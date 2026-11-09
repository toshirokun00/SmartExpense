package com.smartexpense.dashoard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartexpense.dashoard.effect.DashboardEffect
import com.smartexpense.dashoard.intent.DashboardIntent
import com.smartexpense.dashoard.state.DashboardUiState
import com.smartexpense.domain.model.BudgetAlertThresholds
import com.smartexpense.domain.usecase.BudgetUseCase
import com.smartexpense.domain.usecase.CategoryUseCase
import com.smartexpense.domain.usecase.GetBudgetInsightsUseCase
import com.smartexpense.domain.usecase.GetExpenseUseCase
import com.smartexpense.domain.usecase.ProfileUseCase
import com.smartexpense.domain.usecase.SettingsUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withTimeout
import java.time.Instant
import java.time.ZoneId

class DashboardViewModel(
    private val getExpenseUseCase: GetExpenseUseCase,
    private val budgetUseCase: BudgetUseCase,
    private val categoryUseCase: CategoryUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val getBudgetInsightsUseCase: GetBudgetInsightsUseCase,
    private val profileUseCase: ProfileUseCase
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

    private val budgetFlow = budgetUseCase.getBudgets()
        .catch { exception ->
            _effect.emit(
                DashboardEffect.ShowError(
                    exception.message ?: "Failed to load budgets"
                )
            )
        }

    private val categoriesFlow = categoryUseCase.getCategories()
        .catch { exception ->
            _effect.emit(
                DashboardEffect.ShowError(
                    exception.message ?: "Failed to load categories"
                )
            )
        }

    private val settingsFlow = settingsUseCase.getSettings()
        .catch { exception ->
            _effect.emit(
                DashboardEffect.ShowError(
                    exception.message ?: "Failed to load settings"
                )
            )
        }

    private val usernameFlow = kotlinx.coroutines.flow.flow {
        // Hydrate local Room with the remote profile first so a fresh
        // install picks up the user's previously-set nickname.
        // Run with a timeout so a network failure doesn't block the UI.
        try {
            kotlinx.coroutines.withTimeout(5_000L) {
                profileUseCase.hydrateLocalFromRemote()
            }
        } catch (_: Exception) {
            // Remote unavailable — fall through to local data.
        }
        emit(
            profileUseCase
                .observeLocalProfileForCurrentUser()
                ?.username
                ?.takeIf { it.isNotBlank() }
        )
    }.catch {
        // Profile not yet available or no user signed in — emit null.
        emit(null)
    }

    val uiState: StateFlow<DashboardUiState> =
        combine(
            expensesFlow,
            budgetFlow,
            categoriesFlow,
            settingsFlow,
            usernameFlow
        ) { expenses, budgets, categories, settings, username ->

            val totalExpense = expenses.sumOf { it.amount }

            val totalBudget = budgets.sumOf { it.amount }

            val totalBudgetSpent = budgets.sumOf { budget ->
                expenses
                    .filter { expense ->
                        expense.categoryId == budget.categoryId &&
                                isDateWithinRange(
                                    expense.date,
                                    budget.startDate,
                                    budget.endDate
                                )
                    }
                    .sumOf { it.amount }
            }

            val totalBudgetRemaining =
                totalBudget - totalBudgetSpent

            val budgetProgress =
                if (totalBudget > 0) {
                    (totalBudgetSpent / totalBudget)
                        .coerceIn(0.0, 1.0)
                        .toFloat()
                } else {
                    0f
                }

            val warningThreshold = BudgetAlertThresholds.warningThreshold(
                settings.budgetAlertThresholds
            )

            val insights = getBudgetInsightsUseCase(
                budgets = budgets,
                expenses = expenses,
                categories = categories,
                warningThreshold = warningThreshold
            )

            DashboardUiState(
                totalExpense = totalExpense,
                expenseCount = expenses.size,
                totalBudget = totalBudget,
                totalBudgetSpent = totalBudgetSpent,
                totalBudgetRemaining = totalBudgetRemaining,
                budgetProgress = budgetProgress,
                isOverBudget = totalBudgetSpent > totalBudget,
                recentExpenses = expenses.take(5),
                insights = insights,
                isLoading = false,
                username = username
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

    private fun isDateWithinRange(
        expenseDate: Long,
        startDate: Long,
        endDate: Long
    ): Boolean {
        val zoneId = ZoneId.systemDefault()

        val expenseLocalDate =
            Instant.ofEpochMilli(expenseDate)
                .atZone(zoneId)
                .toLocalDate()

        val startLocalDate =
            Instant.ofEpochMilli(startDate)
                .atZone(zoneId)
                .toLocalDate()

        val endLocalDate =
            Instant.ofEpochMilli(endDate)
                .atZone(zoneId)
                .toLocalDate()

        return expenseLocalDate in startLocalDate..endLocalDate
    }
}