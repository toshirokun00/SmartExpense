package com.smartexpense.dashoard.di

import com.smartexpense.dashoard.viewmodel.DashboardViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val dashboardModule = module {

    viewModel {
        DashboardViewModel(
            getExpenseUseCase = get(),
            budgetUseCase = get(),
            categoryUseCase = get(),
            settingsUseCase = get(),
            getBudgetInsightsUseCase = get(),
            profileUseCase = get(),
            getBillsUseCase = get()
        )
    }
}