package com.smartexpense.feature.expenses.di

import com.smartexpense.feature.expenses.viewmodel.ExpenseViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val expenseModule = module {
    viewModel {
        ExpenseViewModel(
            getExpenseUseCase = get(),
            addExpenseUseCase = get(),
            updateExpenseUseCase = get(),
            deleteExpenseUseCase = get(),
            getExpenseByIdUseCase = get(),
            validateExpenseAmountUseCase = get(),
            categoryUseCase = get(),
            settingsUseCase = get(),
            expenseNotificationManager = get())

    }
}