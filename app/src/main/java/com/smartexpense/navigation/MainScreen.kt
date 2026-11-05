package com.smartexpense.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.smartexpene.categories.screen.CategoryScreen
import com.smartexpense.budget.screen.BudgetScreen
import com.smartexpense.dashoard.screen.DashboardScreen
import com.smartexpense.feature.expenses.ui.ExpenseScreen
import com.smartexpense.reports.screen.ReportsScreen
import com.smartexpense.settings.screen.SettingsScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun MainScreen(navController: NavHostController) {
    var selectedTab by remember { mutableStateOf(AppTab.Home) }

    val onTabSelected: (AppTab) -> Unit = remember {
        { tab -> selectedTab = tab }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            ModernBottomNavigation(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate(AddExpenseRoute)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Expense"
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Crossfade(
                targetState = selectedTab,
                animationSpec = tween(durationMillis = 400),
                label = "MainScreenTabTransition"
            ) { tab ->
                when (tab) {
                    AppTab.Home -> DashboardScreen(
                        viewModel = koinViewModel(),
                        onNavigateToExpenses = { onTabSelected(AppTab.Expenses) },
                        onNavigateToTransactions = {
                            navController.navigate(TransactionsRoute)
                        },
                        onNavigateToCategories = {
                            navController.navigate(CategoryRoute)
                        },
                        onNavigateToBudgets = { onTabSelected(AppTab.Budget) },
                        onNavigateToSettings = { onTabSelected(AppTab.Settings) },
                        onNavigateToReports = { onTabSelected(AppTab.Reports) },
                        onNavigateToRecurring = {
                            navController.navigate(RecurringExpensesRoute)
                        }
                    )

                    AppTab.Expenses -> ExpenseScreen(
                        viewModel = koinViewModel(),
                        onAddExpenseClick = {
                            navController.navigate(AddExpenseRoute)
                        },
                        onExpenseClick = { expenseId ->
                            navController.navigate(ExpenseDetailRoute(expenseId))
                        },
                        onNavigateBack = { onTabSelected(AppTab.Home) },
                        onAddExpenseCategoryClick = {
                            navController.navigate(CategoryRoute)
                        }
                    )

                    AppTab.Reports -> ReportsScreen(
                        viewModel = koinViewModel()
                    )

                    AppTab.Budget -> BudgetScreen(
                        viewModel = koinViewModel(),
                        onNavigateBack = { onTabSelected(AppTab.Home) }
                    )

                    AppTab.Settings -> SettingsScreen(
                        viewModel = koinViewModel(),
                        onNavigateBack = { onTabSelected(AppTab.Home) },
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
            }
        }
    }
}
