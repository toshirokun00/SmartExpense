package com.smartexpense.feature.expenses.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.usecase.AddExpenseUseCase
import com.smartexpense.domain.usecase.DeleteExpenseUseCase
import com.smartexpense.domain.usecase.GetExpenseByIdUseCase
import com.smartexpense.domain.usecase.GetExpenseUseCase
import com.smartexpense.domain.usecase.UpdateExpenseUseCase
import com.smartexpense.feature.expenses.effect.ExpenseEffect
import com.smartexpense.feature.expenses.intent.ExpenseIntent
import com.smartexpense.feature.expenses.state.ExpenseFormState
import com.smartexpense.feature.expenses.state.ExpenseScreenState
import com.smartexpense.feature.expenses.state.ExpenseUiState
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

class ExpenseViewModel(
    private val getExpenseUseCase: GetExpenseUseCase,
    private val addExpenseUseCase: AddExpenseUseCase,
    private val updateExpenseUseCase: UpdateExpenseUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    private val getExpenseByIdUseCase: GetExpenseByIdUseCase,

    ) : ViewModel() {

    private val _formState = MutableStateFlow(ExpenseFormState())

    private val _screenState = MutableStateFlow(ExpenseScreenState())
    val screenState: StateFlow<ExpenseScreenState> =
        _screenState.asStateFlow()

    private val expensesFlow = getExpenseUseCase()
        .catch { exception ->
            _effect.emit(
                ExpenseEffect.ShowError(
                    message = exception.message ?: "Failed to load expenses"
                )
            )
        }

    private val _effect = MutableSharedFlow<ExpenseEffect>()

    val effect = _effect.asSharedFlow()


    val uiState: StateFlow<ExpenseUiState> =
        combine(expensesFlow, _formState,) { expenses, formState,  ->
            ExpenseUiState(expenses, formState,
                isLoading = false)
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ExpenseUiState(isLoading = true)
            )


    fun onIntent(intent: ExpenseIntent) {
        when (intent) {
            is ExpenseIntent.AddExpenses -> {
                addExpense()
            }


            is ExpenseIntent.UpdateExpense -> {
                updateExpense(intent)
            }

            is ExpenseIntent.AmountChanged -> {
                _formState.value = _formState.value.copy(
                    amount = intent.amount
                )
            }

            is ExpenseIntent.DescriptionChanged -> {
                _formState.value = _formState.value.copy(
                    description = intent.description
                )
            }

            is ExpenseIntent.LoadExpense -> {
                loadExpense(intent.id)
            }

            ExpenseIntent.CancelDeleteExpense -> {

                cancelDeleteExpense()

            }
            ExpenseIntent.ConfirmDeleteExpense -> {
                confirmDeleteExpense()
            }
            is ExpenseIntent.RequestDeleteExpense -> {
                requestDeleteExpense(intent.id)
            }
        }
    }

    fun addExpense() {
        val state = _formState.value

        val amount = state.amount.toDoubleOrNull()

        if (amount == null || amount <= 0 ) {
            viewModelScope.launch {
                _effect.emit(ExpenseEffect.ShowError(
                    message = "Please enter valid amount"
                ))
            }
            return
        }

        val expense: Expense = Expense(
            amount = amount,
            categoryId = 1L,
            description = state.description,
            date = System.currentTimeMillis()
        )
        viewModelScope.launch {
            addExpenseUseCase(expense)
            _effect.emit(ExpenseEffect.ExpenseAdded)

            _formState.value = _formState.value.copy(
                amount = "",
                description = ""
            )
        }
    }

    fun updateExpense(intent: ExpenseIntent.UpdateExpense) {
        val state = _formState.value
        val amount = state.amount.toDoubleOrNull() ?: return

        if (amount <=0) {
            viewModelScope.launch {
                _effect.emit(ExpenseEffect.ShowError("Please enter valid amount"))
            }
            return
        }

        viewModelScope.launch {
            val existingExpense= getExpenseByIdUseCase(intent.id)
            if (existingExpense == null) {
                _effect.emit(
                    ExpenseEffect.ShowError(
                        message = "Expense not found"
                    )
                )
                return@launch
            }

            val updatedExpense = existingExpense.copy(
                amount = amount,
                description = state.description
            )
            updateExpenseUseCase(updatedExpense)

            _effect.emit(
                ExpenseEffect.ExpenseUpdated
            )
        }
    }

    private fun loadExpense(id: Long) {
        viewModelScope.launch {
            val expense = getExpenseByIdUseCase(id)

            if (expense != null) {
                _formState.value = _formState.value.copy(
                    amount = expense.amount.toString(),
                    description = expense.description
                )
            }
        }
    }

    private fun requestDeleteExpense(id: Long) {
        _screenState.update {
            it.copy(
                showDeleteConfirmation = true,
                expenseToDeleteId = id
            )
        }
    }

    private fun cancelDeleteExpense() {
        _screenState.update {
            it.copy(
                showDeleteConfirmation = false,
                expenseToDeleteId = null
            )
        }
    }
    private fun confirmDeleteExpense() {
        val expenseId = _screenState.value.expenseToDeleteId ?: return

        val expense = uiState.value.expenses.find {
            it.id == expenseId
        } ?: return

        viewModelScope.launch {
            deleteExpenseUseCase(expense)

            _effect.emit(ExpenseEffect.ExpenseDeleted)

            _screenState.update {
                it.copy(showDeleteConfirmation = false,
                    expenseToDeleteId = null)
            }


        }
    }


}