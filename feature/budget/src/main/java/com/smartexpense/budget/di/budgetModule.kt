package com.smartexpense.budget.di

import com.smartexpense.budget.alerts.BudgetAlertCoordinator
import com.smartexpense.budget.viewmodel.BudgetViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val budgetModule = module {
    viewModel {
        BudgetViewModel(
            budgetUseCase = get(),
            categoryUseCase = get(),
            getExpenseUseCase = get(),
            settingsUseCase = get(),
            getBudgetInsightsUseCase = get()
        )
    }

    single {
        BudgetAlertCoordinator(
            getExpenseUseCase = get(),
            budgetUseCase = get(),
            categoryUseCase = get(),
            settingsUseCase = get(),
            evaluateBudgetAlertsUseCase = get(),
            budgetAlertRepository = get(),
            notificationManager = get()
        )
    }
}
