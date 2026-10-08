package com.portfolio.smartexpense.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.portfolio.budget.screen.BudgetScreen
import com.smartexpene.categories.screen.CategoryScreen
import com.smartexpense.dashoard.screen.DashboardScreen
import com.smartexpense.feature.expenses.ui.AddExpenseScreen
import com.smartexpense.feature.expenses.ui.ExpenseDetailScreen
import com.smartexpense.feature.expenses.ui.ExpenseScreen


@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
    startDestination = DashboardRoute
    ) {

        composable<DashboardRoute> {
            DashboardScreen(
                onNavigateToExpenses = {
                    navController.navigate(ExpensesRoute)
                },
                onNavigateToCategories = {
                    navController.navigate(CategoryRoute)
                },
                onNavigateToBudgets = {
                    navController.navigate(BudgetRoute)
                }
            )
        }
        composable<ExpensesRoute> {
            ExpenseScreen(
                onAddExpenseClick = {
                     navController.navigate(AddExpenseRoute)
                },
                onExpenseClick = { expenseId ->
                    navController.navigate(ExpenseDetailRoute(expenseId))
                },
                onAddExpenseCategoryClick = {
                    navController.navigate(CategoryRoute)
                },
                onNavigateBack = {
                    navController.popBackStack()
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


        composable<CategoryRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<CategoryRoute>()

            CategoryScreen(
                onBackClick = {
                    navController.popBackStack()
                },
            )
        }

        composable<BudgetRoute> {
            BudgetScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}