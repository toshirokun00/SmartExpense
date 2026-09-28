package com.smartexpense.feature.expenses.effect

sealed interface ExpenseEffect {

    data object ExpenseAdded : ExpenseEffect

    data object ExpenseDeleted : ExpenseEffect

    data object ExpenseUpdated : ExpenseEffect

    data class ShowError(val message : String) : ExpenseEffect

    data object NavigateToAddExpense: ExpenseEffect
}