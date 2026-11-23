package com.smartexpense.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.smartexpense.R
import com.smartexpense.budget.screen.BudgetFormScreen
import com.smartexpense.budget.screen.BudgetScreen
import com.smartexpense.bills.screen.BillFormScreen
import com.smartexpense.bills.screen.BillHistoryScreen
import com.smartexpense.bills.screen.BillsScreen
import com.smartexpense.paycheck.screen.MoneyTrackerScreen
import com.smartexpense.paycheck.screen.PaycheckFormScreen
import com.smartexpense.paycheck.screen.PaychecksScreen
import com.smartexpense.reports.screen.ReportsScreen
import com.smartexpense.profile.screen.AboutScreen
import com.smartexpense.settings.screen.SettingsScreen
import com.smartexpense.SplashScreen
import com.smartexpense.StartupViewModel
import com.smartexpense.designsystems.LivelyToast
import com.smartexpense.domain.network.NO_INTERNET_MESSAGE
import com.smartexpense.network.AndroidNetworkStatusProvider
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
import org.koin.compose.koinInject


@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val startupViewModel: StartupViewModel = koinViewModel()
    val startupState by startupViewModel.uiState.collectAsStateWithLifecycle()

    val startDestination = startupState.startDestination

    // Connectivity is observed once for the whole app: when the device is
    // offline, any screen trying to load data shows the same clear notice.
    val networkStatus = koinInject<AndroidNetworkStatusProvider>()
    val isConnected by networkStatus.connected.collectAsStateWithLifecycle()
    var offlineNoticeDismissed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(isConnected) {
        if (isConnected) offlineNoticeDismissed = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
    // Fade the splash into the first real destination instead of swapping it
    // abruptly. startDestination resolves only once, so the NavHost itself is
    // not recreated.
    Crossfade(
        targetState = startDestination,
        animationSpec = tween(durationMillis = 350),
        label = "StartupTransition"
    ) { destination ->
        if (destination == null) {
            SplashScreen()
            return@Crossfade
        }

        NavHost(
            navController = navController,
            startDestination = destination,
            // Paint the themed background behind every destination. Some
            // screens (e.g. Settings) have a transparent root, which would
            // otherwise reveal the light window background in dark mode.
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            // Shared-axis style motion: the outgoing screen drifts out while
            // the incoming screen fades and slides in. Pop reverses it.
            enterTransition = {
                fadeIn(tween(durationMillis = 260, delayMillis = 40)) +
                    slideInHorizontally(tween(durationMillis = 340)) { it / 4 }
            },
            exitTransition = {
                fadeOut(tween(durationMillis = 200)) +
                    slideOutHorizontally(tween(durationMillis = 340)) { -it / 4 }
            },
            popEnterTransition = {
                fadeIn(tween(durationMillis = 260, delayMillis = 40)) +
                    slideInHorizontally(tween(durationMillis = 340)) { -it / 4 }
            },
            popExitTransition = {
                fadeOut(tween(durationMillis = 200)) +
                    slideOutHorizontally(tween(durationMillis = 340)) { it / 4 }
            }
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
                    onHistory = { navController.navigate(BillHistoryRoute) },
                    onEditBill = { billId ->
                        navController.navigate(EditBillRoute(billId))
                    }
                )
            }

            composable<BillHistoryRoute> {
                BillHistoryScreen(
                    onNavigateBack = { navController.popBackStack() }
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

            composable<PaychecksRoute> {
                PaychecksScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onAddPaycheck = { navController.navigate(AddPaycheckRoute) },
                    onPaycheckClick = { paycheckId ->
                        navController.navigate(EditPaycheckRoute(paycheckId))
                    }
                )
            }

            composable<MoneyTrackerRoute> {
                MoneyTrackerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onPaycheckClick = { paycheckId ->
                        navController.navigate(EditPaycheckRoute(paycheckId))
                    }
                )
            }

            composable<AddPaycheckRoute> {
                PaycheckFormScreen(
                    paycheckId = null,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onSaved = {
                        navController.popBackStack()
                    },
                    onDeleted = {
                        navController.popBackStack()
                    }
                )
            }

            composable<EditPaycheckRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<EditPaycheckRoute>()

                PaycheckFormScreen(
                    paycheckId = route.paycheckId,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onSaved = {
                        navController.popBackStack()
                    },
                    onDeleted = {
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
                    logoRes = R.mipmap.app_logo,
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

    AnimatedVisibility(
        visible = !isConnected && !offlineNoticeDismissed,
        enter = fadeIn(tween(durationMillis = 180)) +
            slideInVertically(tween(durationMillis = 240)) { it / 5 },
        exit = fadeOut(tween(durationMillis = 150)) +
            slideOutVertically(tween(durationMillis = 180)) { it / 5 },
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        label = "OfflineToast"
    ) {
        LivelyToast(
            message = NO_INTERNET_MESSAGE,
            isError = true,
            onDismiss = { offlineNoticeDismissed = true }
        )
    }
    }
}