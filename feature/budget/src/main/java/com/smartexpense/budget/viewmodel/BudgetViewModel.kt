package com.smartexpense.budget.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartexpense.budget.effect.BudgetEffect
import com.smartexpense.budget.intent.BudgetIntent
import com.smartexpense.budget.state.BudgetFormState
import com.smartexpense.budget.state.BudgetScreenState
import com.smartexpense.budget.state.BudgetSummaryState
import com.smartexpense.budget.state.BudgetUiState
import com.smartexpense.domain.budget.BudgetCalculator
import com.smartexpense.domain.model.Budget
import com.smartexpense.domain.model.BudgetAlertThresholds
import com.smartexpense.domain.usecase.BudgetUseCase
import com.smartexpense.domain.usecase.CategoryUseCase
import com.smartexpense.domain.usecase.GetBudgetInsightsUseCase
import com.smartexpense.domain.usecase.GetExpenseUseCase
import com.smartexpense.domain.usecase.SettingsUseCase
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

class BudgetViewModel(
    private val budgetUseCase: BudgetUseCase,
    private val categoryUseCase: CategoryUseCase,
    private val getExpenseUseCase: GetExpenseUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val getBudgetInsightsUseCase: GetBudgetInsightsUseCase
) : ViewModel() {

    private val _formState = MutableStateFlow(
        BudgetFormState()
    )

    private val _screenState = MutableStateFlow(
        BudgetScreenState()
    )

    val screenState = _screenState.asStateFlow()

    private val _effect = MutableSharedFlow<BudgetEffect>()

    val effect = _effect.asSharedFlow()

    private val budgetsFlow = budgetUseCase.getBudgets()
        .catch { exception ->
            _effect.emit(
                BudgetEffect.ShowError(
                    exception.message
                        ?: "Failed to load budgets"
                )
            )
        }

    private val categoriesFlow = categoryUseCase.getCategories()
        .catch { exception ->
            _effect.emit(
                BudgetEffect.ShowError(
                    exception.message
                        ?: "Failed to load categories"
                )
            )
        }

    private val expensesFlow =
        getExpenseUseCase()
            .catch {
                _effect.emit(
                    BudgetEffect.ShowError(
                        it.message ?: "Failed to load expenses"
                    )
                )
            }

    private val settingsFlow =
        settingsUseCase.getSettings()
            .catch {
                _effect.emit(
                    BudgetEffect.ShowError(
                        it.message ?: "Failed to load settings"
                    )
                )
            }

    val uiState: StateFlow<BudgetUiState> =
        combine(
            budgetsFlow,
            categoriesFlow,
            expensesFlow,
            settingsFlow,
            _formState
        ) { budgets, categories, expenses, settings, form ->

            val warningThreshold =
                BudgetAlertThresholds.warningThreshold(
                    settings.budgetAlertThresholds
                )

            val summaries = budgets.map { budget ->
                val utilization = BudgetCalculator.utilization(
                    budget = budget,
                    expenses = expenses,
                    warningThreshold = warningThreshold
                )

                val categoryName =
                    categories
                        .firstOrNull { it.id == budget.categoryId }
                        ?.name
                        ?: "Unknown"

                BudgetSummaryState(
                    budget = budget,
                    categoryName = categoryName,
                    spentAmount = utilization.spentAmount,
                    remainingAmount = utilization.remainingAmount,
                    progress = utilization.progress,
                    isOverBudget = utilization.isOverBudget,
                    utilizationPercentage = utilization.utilizationPercentage,
                    status = utilization.status
                )
            }

            val insights = getBudgetInsightsUseCase(
                budgets = budgets,
                expenses = expenses,
                categories = categories,
                warningThreshold = warningThreshold
            )

            BudgetUiState(
                budgetSummaries = summaries,
                insights = insights,
                categories = categories,
                form = form
            )

        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BudgetUiState()
        )

    fun onIntent(intent: BudgetIntent) {
        when (intent) {

            is BudgetIntent.CategoryChanged ->
                _formState.update {
                    it.copy(
                        categoryId = intent.categoryId
                    )
                }

            is BudgetIntent.AmountChanged ->
                _formState.update {
                    it.copy(
                        amount = intent.amount
                    )
                }

            is BudgetIntent.StartDateChanged ->
                _formState.update {
                    it.copy(
                        startDate = intent.date
                    )
                }

            is BudgetIntent.EndDateChanged ->
                _formState.update {
                    it.copy(
                        endDate = intent.date
                    )
                }

            BudgetIntent.AddBudget ->
                addBudget()

            is BudgetIntent.LoadBudget ->
                loadBudget(intent.id)

            BudgetIntent.UpdateBudget ->
                updateBudget()

            BudgetIntent.CancelEdit ->
                _formState.value = BudgetFormState()

            is BudgetIntent.RequestDeleteBudget ->
                requestDeleteBudget(intent.id)

            BudgetIntent.ConfirmDeleteBudget ->
                confirmDeleteBudget()

            BudgetIntent.CancelDeleteBudget ->
                cancelDeleteBudget()
        }
    }

    private fun addBudget() {

        val state = _formState.value

        val amount = state.amount.toDoubleOrNull()

        if (amount == null || amount <= 0) {
            showError("Please enter a valid budget amount")
            return
        }

        if (state.categoryId == null) {
            showError("Please select a category")
            return
        }

        if (state.startDate == null) {
            showError("Please select a start date")
            return
        }

        if (state.endDate == null) {
            showError("Please select an end date")
            return
        }

        if (state.endDate < state.startDate) {
            showError("End date cannot be before start date")
            return
        }

        viewModelScope.launch {

            budgetUseCase.addBudget(
                Budget(
                    categoryId = state.categoryId,
                    amount = amount,
                    startDate = state.startDate,
                    endDate = state.endDate
                )
            )

            _formState.value = BudgetFormState()

            _effect.emit(
                BudgetEffect.BudgetAdded
            )
        }
    }

    private fun loadBudget(id: Long) {

        viewModelScope.launch {

            val budget = budgetUseCase.getBudgetById(id)

            if (budget == null) {
                _effect.emit(
                    BudgetEffect.ShowError(
                        "Budget not found"
                    )
                )
                return@launch
            }

            _formState.value = BudgetFormState(
                categoryId = budget.categoryId,
                amount = budget.amount.toString(),
                startDate = budget.startDate,
                endDate = budget.endDate,
                editingBudgetId = budget.id
            )
        }
    }

    private fun updateBudget() {

        val state = _formState.value

        val budgetId = state.editingBudgetId

        val amount = state.amount.toDoubleOrNull()

        if (budgetId == null) {
            showError("No budget selected for editing")
            return
        }

        if (amount == null || amount <= 0) {
            showError("Please enter a valid budget amount")
            return
        }

        if (state.categoryId == null) {
            showError("Please select a category")
            return
        }

        if (state.startDate == null) {
            showError("Please select a start date")
            return
        }

        if (state.endDate == null) {
            showError("Please select an end date")
            return
        }

        if (state.endDate < state.startDate) {
            showError("End date cannot be before start date")
            return
        }

        viewModelScope.launch {

            val existingBudget =
                budgetUseCase.getBudgetById(budgetId)

            if (existingBudget == null) {
                _effect.emit(
                    BudgetEffect.ShowError(
                        "Budget not found"
                    )
                )
                return@launch
            }

            budgetUseCase.updateBudget(
                existingBudget.copy(
                    categoryId = state.categoryId,
                    amount = amount,
                    startDate = state.startDate,
                    endDate = state.endDate
                )
            )

            _formState.value = BudgetFormState()

            _effect.emit(
                BudgetEffect.BudgetUpdated
            )
        }
    }

    private fun requestDeleteBudget(id: Long) {

        _screenState.update {
            it.copy(
                showDeleteConfirmation = true,
                budgetToDeleteId = id
            )
        }
    }

    private fun confirmDeleteBudget() {

        val budgetId =
            _screenState.value.budgetToDeleteId
                ?: return

        viewModelScope.launch {

            val budget =
                budgetUseCase.getBudgetById(budgetId)

            if (budget == null) {
                _effect.emit(
                    BudgetEffect.ShowError(
                        "Budget not found"
                    )
                )

                cancelDeleteBudget()
                return@launch
            }

            budgetUseCase.deleteBudget(budget)

            _effect.emit(
                BudgetEffect.BudgetDeleted
            )

            cancelDeleteBudget()
        }
    }

    private fun cancelDeleteBudget() {

        _screenState.update {
            it.copy(
                showDeleteConfirmation = false,
                budgetToDeleteId = null
            )
        }
    }

    private fun showError(message: String) {

        viewModelScope.launch {
            _effect.emit(
                BudgetEffect.ShowError(message)
            )
        }
    }
}
