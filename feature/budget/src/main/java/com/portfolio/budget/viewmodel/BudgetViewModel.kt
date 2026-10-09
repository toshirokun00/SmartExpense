package com.portfolio.budget.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.budget.effect.BudgetEffect
import com.portfolio.budget.intent.BudgetIntent
import com.portfolio.budget.state.BudgetFormState
import com.portfolio.budget.state.BudgetScreenState
import com.portfolio.budget.state.BudgetSummaryState
import com.portfolio.budget.state.BudgetUiState
import com.smartexpense.domain.model.Budget
import com.smartexpense.domain.usecase.BudgetUseCase
import com.smartexpense.domain.usecase.CategoryUseCase
import com.smartexpense.domain.usecase.GetExpenseUseCase
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
import java.time.Instant
import java.time.ZoneId

class BudgetViewModel(
    private val budgetUseCase: BudgetUseCase,
    private val categoryUseCase: CategoryUseCase,
    private val getExpenseUseCase: GetExpenseUseCase
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

    val uiState: StateFlow<BudgetUiState> =
        combine(
          budgetUseCase.getBudgets(),
            categoryUseCase.getCategories(),
            expensesFlow,
            _formState
        ) {  budgets, categories, expenses, form  ->

            val summaries = budgets.map { budget ->

                val spentAmount = expenses
                    .filter { expense ->
                        expense.categoryId == budget.categoryId &&
                                isDateWithinRange(
                                    expense.date,
                                    budget.startDate,
                                    budget.endDate
                                )
                    }
                    .sumOf { it.amount }

                val remainingAmount =
                    budget.amount - spentAmount

                val progress =
                    if (budget.amount > 0) {
                        (spentAmount / budget.amount)
                            .coerceIn(0.0, 1.0)
                            .toFloat()
                    } else {
                        0f
                    }

                val categoryName =
                    categories
                        .firstOrNull { it.id == budget.categoryId }
                        ?.name
                        ?: "Unknown"


                BudgetSummaryState(
                    budget = budget,
                    categoryName = categoryName,
                    spentAmount = spentAmount,
                    remainingAmount = remainingAmount,
                    progress = progress,
                    isOverBudget = spentAmount > budget.amount
                )

            }

            BudgetUiState(
                budgets = budgets,
                budgetSummaries = summaries,
                categories = categories,
                form = form,
                isLoading = false
            )

        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5_000),
            initialValue = BudgetUiState(
                isLoading = true
            )
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