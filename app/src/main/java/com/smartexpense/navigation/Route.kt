package com.smartexpense.navigation

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
data object CategoryRoute : Route


@Serializable
data object DashboardRoute : Route

@Serializable
data object BudgetRoute : Route

@Serializable
data object SettingsRoute : Route

@Serializable
data object ReportsRoute : Route

@Serializable
data object AuthRoute : Route

@Serializable
data object EmailConfirmationRoute

@Serializable
data object SplashRoute : Route