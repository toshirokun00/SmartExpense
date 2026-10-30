package com.smartexpense.feature.expenses.di

import com.smartexpense.feature.expenses.viewmodel.ExpenseViewModel
import com.smartexpense.feature.expenses.viewmodel.TransactionViewModel
import com.smartexpense.domain.usecase.SearchExpensesUseCase
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
            scheduleSyncUseCase = get(),
            expenseNotificationManager = get())
    }

    viewModel {
        TransactionViewModel(
            searchExpensesUseCase = get(),
            categoryUseCase = get(),
            scheduleSyncUseCase = get())
    }
}