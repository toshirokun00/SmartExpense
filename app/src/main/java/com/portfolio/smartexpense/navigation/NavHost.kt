package com.portfolio.smartexpense.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.smartexpense.feature.expenses.ui.AddExpenseScreen
import com.smartexpense.feature.expenses.ui.ExpenseDetailScreen
import com.smartexpense.feature.expenses.ui.ExpenseScreen


@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
    startDestination = ExpensesRoute
    ) {
        composable<ExpensesRoute> {
            ExpenseScreen(
                onAddExpenseClick = {
                     navController.navigate(AddExpenseRoute)
                },
                onExpenseClick = { expenseId ->
                    navController.navigate(ExpenseDetailRoute(expenseId))
                }
            )
        }
        composable<AddExpenseRoute> {
            AddExpenseScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable<ExpenseDetailRoute> { backStackEntry ->

            val route = backStackEntry.toRoute<ExpenseDetailRoute>()
            ExpenseDetailScreen(
                expenseId = route.expenseId,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}