package com.smartexpense.feature.expenses.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartexpense.core.common.util.MoneyInputUtil
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.money.Money
import com.smartexpense.domain.usecase.AddExpenseUseCase
import com.smartexpense.domain.usecase.CategoryUseCase
import com.smartexpense.domain.usecase.DeleteExpenseUseCase
import com.smartexpense.domain.usecase.GetExpenseByIdUseCase
import com.smartexpense.domain.usecase.GetExpenseUseCase
import com.smartexpense.domain.usecase.ScheduleSyncUseCase
import com.smartexpense.domain.usecase.SettingsUseCase
import com.smartexpense.domain.usecase.UpdateExpenseUseCase
import com.smartexpense.domain.usecase.ValidateAmountUseCase
import com.smartexpense.feature.expenses.effect.ExpenseEffect
import com.smartexpense.feature.expenses.intent.ExpenseIntent
import com.smartexpense.feature.expenses.state.ExpenseFormState
import com.smartexpense.feature.expenses.state.ExpenseScreenState
import com.smartexpense.feature.expenses.state.ExpenseUiState
import com.smartexpense.notification.ExpenseNotificationManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExpenseViewModel(
    private val getExpenseUseCase: GetExpenseUseCase,
    private val addExpenseUseCase: AddExpenseUseCase,
    private val updateExpenseUseCase: UpdateExpenseUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    private val getExpenseByIdUseCase: GetExpenseByIdUseCase,
    private val validateExpenseAmountUseCase: ValidateAmountUseCase,
    private val categoryUseCase: CategoryUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val scheduleSyncUseCase: ScheduleSyncUseCase,
    private val expenseNotificationManager: ExpenseNotificationManager
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

    private val categoriesFlow = categoryUseCase.getCategories()
        .catch { exception ->
            _effect.emit(
                ExpenseEffect.ShowError(
                    exception.message ?: "Failed to load categories"
                )
            )
        }


    val uiState: StateFlow<ExpenseUiState> =
        combine(
            expensesFlow,
            categoriesFlow,
            _formState,
        ) { expenses, categories, formState ->
            ExpenseUiState(
                expenses,
                categories,
                formState,
                totalAmount = Money.toDouble(expenses.sumOf { it.amountMinor }),
                isLoading = false
            )
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
                val formatted = MoneyInputUtil.formatAmountInput(
                    input = intent.amount,
                    previousInput = _formState.value.amount
                )
                _formState.value = _formState.value.copy(
                    amount = formatted
                )
            }

            is ExpenseIntent.DescriptionChanged -> {
                _formState.value = _formState.value.copy(
                    description = intent.description
                )
            }

            is ExpenseIntent.MerchantChanged -> {
                _formState.update {
                    it.copy(merchant = intent.merchant)
                }
            }

            is ExpenseIntent.DateChanged -> {
                _formState.update {
                    it.copy(date = intent.date)
                }
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

            is ExpenseIntent.RetrySync -> {
                retrySync(intent.id)
            }

            is ExpenseIntent.CategoryChanged -> {

                _formState.update {
                    it.copy(
                        categoryId = intent.categoryId
                    )
                }

            }
        }
    }

    fun addExpense() {
        if (_formState.value.isSaving) return
        val state = _formState.value

        val amount = validateExpenseAmountUseCase(state.amount)
        if (amount == null) {
            viewModelScope.launch {
                _effect.emit(ExpenseEffect.ShowError("Please enter valid amount"))
            }
            return
        }
        val categoryId = state.categoryId
        if (categoryId == null) {
            viewModelScope.launch {
                _effect.emit(ExpenseEffect.ShowError("Please select a category"))
            }
            return
        }

        val expense = Expense(
            amount = amount,
            categoryId = categoryId,
            description = state.description,
            date = state.date,
            merchant = state.merchant
        )
        viewModelScope.launch {
            _formState.update { it.copy(isSaving = true) }
            try {
                // This persists locally; remote sync happens asynchronously and
                // must not prevent successful local capture while offline.
                addExpenseUseCase(expense)
                _effect.emit(ExpenseEffect.ExpenseAdded)
                _formState.value = ExpenseFormState()

                try {
                    val settings = settingsUseCase.getSettings().first()
                    if (settings.notificationsEnabled) {
                        expenseNotificationManager.showExpenseAddedNotification(
                            description = expense.merchant.ifBlank {
                                expense.description.ifBlank { "New expense" }
                            },
                            amount = "₱${Money.format(expense.amountMinor)}"
                        )
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    // Notification failure must not turn a successful save into
                    // an expense save error.
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _effect.emit(
                    ExpenseEffect.ShowError(
                        message = exception.message ?: "Failed to save expense"
                    )
                )
            } finally {
                _formState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun updateExpense(intent: ExpenseIntent.UpdateExpense) {
        if (_formState.value.isSaving) return
        val state = _formState.value
        val amount = validateExpenseAmountUseCase(state.amount)

        if (amount == null) {
            viewModelScope.launch {
                _effect.emit(ExpenseEffect.ShowError("Please enter valid amount"))
            }
            return
        }

        val categoryId = state.categoryId

        if (categoryId == null) {
            viewModelScope.launch {
                _effect.emit(
                    ExpenseEffect.ShowError(
                        "Please select category"
                    )
                )
            }
            return
        }
        viewModelScope.launch {
            _formState.update { it.copy(isSaving = true) }
            try {
                val existingExpense = getExpenseByIdUseCase(intent.id)
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
                    description = state.description,
                    merchant = state.merchant,
                    categoryId = categoryId,
                    date = state.date
                )
                updateExpenseUseCase(updatedExpense)

                _effect.emit(
                    ExpenseEffect.ExpenseUpdated
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _effect.emit(
                    ExpenseEffect.ShowError(
                        message = exception.message ?: "Failed to update expense"
                    )
                )
            } finally {
                _formState.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun loadExpense(id: Long) {
        viewModelScope.launch {
            try {
                val expense = getExpenseByIdUseCase(id)

                if (expense != null) {
                    _formState.value = _formState.value.copy(
                        amount = MoneyInputUtil.formatAmountInput(Money.format(expense.amountMinor)),
                        description = expense.description,
                        merchant = expense.merchant,
                        categoryId = expense.categoryId,
                        date = expense.date,
                        syncStatus = expense.syncStatus
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _effect.emit(
                    ExpenseEffect.ShowError(
                        exception.message ?: "Failed to load expense"
                    )
                )
            }
        }
    }

    private fun retrySync(id: Long) {
        // The worker already picks up FAILED rows, so a manual retry only needs
        // to ask for another sync run.
        scheduleSyncUseCase()
        viewModelScope.launch {
            _effect.emit(ExpenseEffect.SyncRetryRequested)
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
            try {
                deleteExpenseUseCase(expense)

                _effect.emit(ExpenseEffect.ExpenseDeleted)

                _screenState.update {
                    it.copy(
                        showDeleteConfirmation = false,
                        expenseToDeleteId = null
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _effect.emit(
                    ExpenseEffect.ShowError(
                        exception.message ?: "Failed to delete expense"
                    )
                )
            }
        }
    }


}
