package com.portfolio.smartexpense.navigation

import kotlinx.serialization.Serializable


sealed interface Route

@Serializable
data object ExpensesRoute : Route

@Serializable
data object
AddExpenseRoute : Route

@Serializable
data class ExpenseDetailRoute(
    val expenseId: Long
) : Route

@Serializable
data object CategoryRoute

