package com.portfolio.budget.intent

sealed interface BudgetIntent {

    data class CategoryChanged(
        val categoryId: Long
    ) : BudgetIntent

    data class AmountChanged(
        val amount: String
    ) : BudgetIntent

    data class StartDateChanged(
        val date: Long
    ) : BudgetIntent

    data class EndDateChanged(
        val date: Long
    ) : BudgetIntent

    data object AddBudget : BudgetIntent

    data class LoadBudget(
        val id: Long
    ) : BudgetIntent

    data object UpdateBudget : BudgetIntent

    data object CancelEdit : BudgetIntent

    data class RequestDeleteBudget(
        val id: Long
    ) : BudgetIntent

    data object ConfirmDeleteBudget : BudgetIntent

    data object CancelDeleteBudget : BudgetIntent
}