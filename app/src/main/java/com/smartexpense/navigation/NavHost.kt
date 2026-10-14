package com.smartexpense.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.portfolio.budget.screen.BudgetScreen
import com.portfolio.reports.screen.ReportsScreen
import com.portfolio.settings.screen.SettingsScreen
import com.smartexpense.SplashScreen
import com.smartexpense.StartupViewModel
import com.smartexpene.categories.screen.CategoryScreen
import com.smartexpense.auth.screen.AuthScreen
import com.smartexpense.auth.screen.EmailConfirmationScreen
import com.smartexpense.dashoard.screen.DashboardScreen
import com.smartexpense.feature.expenses.ui.AddExpenseScreen
import com.smartexpense.feature.expenses.ui.ExpenseDetailScreen
import com.smartexpense.feature.expenses.ui.ExpenseScreen
import org.koin.androidx.compose.koinViewModel


@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val startupViewModel: StartupViewModel = koinViewModel()
    val startupState by startupViewModel.uiState.collectAsStateWithLifecycle()

    val startDestination = startupState.startDestination

    if (startDestination == null) {
        SplashScreen()
    } else {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {

            composable<SplashRoute> {
                // Check the current authentication session.
                SplashScreen()
            }

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
                    },
                    onNavigateToSettings = {
                        navController.navigate(SettingsRoute)
                    },
                    onNavigateToReports = {
                        navController.navigate(ReportsRoute)
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

            composable<SettingsRoute> {
                SettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToSignIn = {
                        navController.navigate(AuthRoute) {
                            popUpTo(navController.graph.id) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable<ReportsRoute> {
                ReportsScreen()
            }

            composable<AuthRoute> {
                AuthScreen(
                    onNavigateToHome = {
                        navController.navigate(DashboardRoute) {
                            popUpTo<AuthRoute> {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToEmailConfirmation = {
                        navController.navigate(EmailConfirmationRoute) {
                            popUpTo<AuthRoute> {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable<EmailConfirmationRoute> {
                EmailConfirmationScreen(
                    onBackToSignIn = {
                        navController.navigate(AuthRoute) {
                            popUpTo<EmailConfirmationRoute> {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }

}