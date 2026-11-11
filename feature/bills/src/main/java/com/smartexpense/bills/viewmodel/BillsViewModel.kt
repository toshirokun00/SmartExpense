package com.smartexpense.bills.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartexpense.bills.intent.BillsIntent
import com.smartexpense.bills.state.BillsUiState
import com.smartexpense.domain.usecase.CategoryUseCase
import com.smartexpense.domain.usecase.GetBillsUseCase
import com.smartexpense.domain.usecase.PayBillUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BillsViewModel(
    private val getBillsUseCase: GetBillsUseCase,
    private val payBillUseCase: PayBillUseCase,
    private val categoryUseCase: CategoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BillsUiState())
    val uiState: StateFlow<BillsUiState> = _uiState

    init {
        viewModelScope.launch {
            getBillsUseCase().collect { bills ->
                _uiState.update { it.copy(bills = bills, isLoading = false) }
            }
        }
        viewModelScope.launch {
            categoryUseCase.getCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    fun onIntent(intent: BillsIntent) {
        when (intent) {
            BillsIntent.Load -> Unit

            is BillsIntent.ShowPaySheet -> _uiState.update {
                it.copy(payingBill = intent.bill)
            }

            BillsIntent.HidePaySheet -> _uiState.update {
                it.copy(payingBill = null)
            }

            is BillsIntent.Pay -> viewModelScope.launch {
                val bill = _uiState.value.bills.firstOrNull { it.id == intent.id }
                    ?: _uiState.value.payingBill
                    ?: return@launch
                val categoryId = bill.categoryId
                    ?: _uiState.value.categories.firstOrNull()?.id
                    ?: 0L
                payBillUseCase(
                    bill = bill,
                    amountPaid = intent.amount,
                    paidAtMillis = intent.paidAtMillis,
                    paymentMethod = intent.paymentMethod,
                    createExpense = intent.createExpense,
                    expenseCategoryId = categoryId
                )
                _uiState.update { it.copy(payingBill = null) }
            }
        }
    }
}
