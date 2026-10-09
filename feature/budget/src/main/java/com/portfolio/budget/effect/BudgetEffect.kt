package com.portfolio.budget.effect

sealed interface
BudgetEffect {

    data object BudgetAdded : BudgetEffect

    data object BudgetUpdated : BudgetEffect

    data object BudgetDeleted : BudgetEffect

    data class ShowError(
        val message: String
    ) : BudgetEffect
}