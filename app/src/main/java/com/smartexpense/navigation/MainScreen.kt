package com.smartexpense.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.smartexpene.categories.screen.CategoryScreen
import com.smartexpense.budget.screen.BudgetScreen
import com.smartexpense.dashoard.screen.DashboardScreen
import com.smartexpense.feature.expenses.ui.ExpenseScreen
import com.smartexpense.reports.screen.ReportsScreen
import com.smartexpense.profile.screen.ProfileScreen
import com.smartexpense.settings.screen.SettingsScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun MainScreen(navController: NavHostController) {
    // Persist selected tab across navigation pops (e.g. returning from Add Budget
    // back to Budget tab). rememberSaveable keeps the tab value in the Bundle so
    // it survives being removed from and re-added to the composition tree.
    val selectedTabState = rememberSaveable(
        stateSaver = AppTabSaver,
        init = { mutableStateOf(AppTab.Home) }
    )
    var selectedTab by selectedTabState

    val onTabSelected: (AppTab) -> Unit = remember {
        { tab -> selectedTab = tab }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                ModernBottomNavigation(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected
                )
            }
        },
        // The tab content draws edge-to-edge so each screen's header can sit
        // behind the status bar (like the Bills and Auth screens do). The
        // bottom bar keeps its own navigation-bar inset.
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
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
                        onNavigateToSettings = { onTabSelected(AppTab.Profile) },
                        onNavigateToReports = { onTabSelected(AppTab.Reports) },
                        onNavigateToRecurring = {
                            navController.navigate(RecurringExpensesRoute)
                        },
                        onNavigateToBills = {
                            navController.navigate(BillsRoute)
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
                        onAddExpenseCategoryClick = {
                            navController.navigate(CategoryRoute)
                        }
                    )

                    AppTab.Reports -> ReportsScreen(
                        viewModel = koinViewModel()
                    )

                    AppTab.Budget -> BudgetScreen(
                        viewModel = koinViewModel(),
                        onAddBudgetClick = {
                            navController.navigate(AddBudgetRoute)
                        },
                        onEditBudgetClick = { budgetId ->
                            navController.navigate(EditBudgetRoute(budgetId))
                        }
                    )

                    AppTab.Profile -> ProfileScreen(
                        onNavigateToSettings = {
                            navController.navigate(SettingsRoute)
                        },
                        onNavigateToAbout = {
                            navController.navigate(ProfileAboutRoute)
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
            }
        }
    }
}
