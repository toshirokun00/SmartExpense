package com.smartexpense.feature.expenses.state

import com.smartexpense.domain.model.Category
import com.smartexpense.domain.model.Expense

data class ExpenseUiState(
    val expenses : List<Expense> = emptyList(),
    val categories: List<Category> = emptyList(),
    val form: ExpenseFormState = ExpenseFormState(),
    val isLoading : Boolean = false,
    val error : String? = null
)