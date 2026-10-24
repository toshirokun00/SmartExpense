package com.smartexpense.recurring.di

import com.smartexpense.recurring.viewmodel.RecurringExpenseFormViewModel
import com.smartexpense.recurring.viewmodel.RecurringExpenseListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val recurringModule = module {

    viewModel {
        RecurringExpenseListViewModel(
            recurringExpenseUseCase = get(),
            categoryUseCase = get(),
            processRecurringExpensesUseCase = get()
        )
    }

    viewModel {
        RecurringExpenseFormViewModel(
            recurringExpenseUseCase = get(),
            categoryUseCase = get(),
            clock = get()
        )
    }
}
