package com.smartexpense.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.smartexpense.R
import com.smartexpense.budget.screen.BudgetFormScreen
import com.smartexpense.budget.screen.BudgetScreen
import com.smartexpense.bills.screen.BillFormScreen
import com.smartexpense.bills.screen.BillsScreen
import com.smartexpense.reports.screen.ReportsScreen
import com.smartexpense.profile.screen.AboutScreen
import com.smartexpense.settings.screen.SettingsScreen
import com.smartexpense.SplashScreen
import com.smartexpense.StartupViewModel
import com.smartexpene.categories.screen.CategoryScreen
import com.smartexpense.auth.di.authFeatureModule
import com.smartexpense.auth.screen.AuthScreen
import com.smartexpense.auth.screen.EmailConfirmationScreen
import com.smartexpense.feature.expenses.ui.AddExpenseScreen
import com.smartexpense.feature.expenses.ui.ExpenseDetailScreen
import com.smartexpense.feature.expenses.ui.ExpenseScreen
import com.smartexpense.feature.expenses.screen.TransactionScreen
import com.smartexpense.recurring.screen.RecurringExpenseFormScreen
import com.smartexpense.recurring.screen.RecurringExpenseListScreen
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
            startDestination = startDestination,
            // Paint the themed background behind every destination. Some
            // screens (e.g. Settings) have a transparent root, which would
            // otherwise reveal the light window background in dark mode.
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {

            composable<SplashRoute> {
                // Check the current authentication session.
                SplashScreen()
            }

            composable<DashboardRoute> {
                MainScreen(
                    navController = navController
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
                    }
                )
            }
            composable<TransactionsRoute> {
                TransactionScreen(
                    onExpenseClick = { expenseId ->
                        navController.navigate(ExpenseDetailRoute(expenseId))
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
                    onAddBudgetClick = {
                        navController.navigate(AddBudgetRoute)
                    },
                    onEditBudgetClick = { budgetId ->
                        navController.navigate(EditBudgetRoute(budgetId))
                    }
                )
            }

            composable<AddBudgetRoute> {
                BudgetFormScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onSaved = {
                        navController.popBackStack()
                    }
                )
            }

            composable<EditBudgetRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<EditBudgetRoute>()

                BudgetFormScreen(
                    budgetId = route.budgetId,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onSaved = {
                        navController.popBackStack()
                    }
                )
            }

            composable<ProfileAboutRoute> {
                AboutScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    logoRes = R.mipmap.app_logo
                )
            }

            composable<SettingsRoute> {
                SettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable<ReportsRoute> {
                ReportsScreen()
            }

            composable<RecurringExpensesRoute> {
                RecurringExpenseListScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onAddSchedule = {
                        navController.navigate(AddRecurringExpenseRoute)
                    },
                    onEditSchedule = { scheduleId ->
                        navController.navigate(
                            EditRecurringExpenseRoute(scheduleId)
                        )
                    }
                )
            }

            composable<BillsRoute> {
                BillsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onAddBill = { navController.navigate(AddBillRoute) },
                    onEditBill = { billId ->
                        navController.navigate(EditBillRoute(billId))
                    }
                )
            }

            composable<AddBillRoute> {
                BillFormScreen(
                    billId = null,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onSaved = {
                        navController.popBackStack()
                    }
                )
            }

            composable<EditBillRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<EditBillRoute>()

                BillFormScreen(
                    billId = route.billId,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onSaved = {
                        navController.popBackStack()
                    }
                )
            }

            composable<AddRecurringExpenseRoute> {
                RecurringExpenseFormScreen(
                    scheduleId = null,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onSaved = {
                        navController.popBackStack()
                    }
                )
            }

            composable<EditRecurringExpenseRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<EditRecurringExpenseRoute>()

                RecurringExpenseFormScreen(
                    scheduleId = route.scheduleId,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onSaved = {
                        navController.popBackStack()
                    }
                )
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