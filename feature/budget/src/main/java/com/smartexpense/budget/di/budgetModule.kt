package com.smartexpense.budget.di

import com.smartexpense.budget.viewmodel.BudgetViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val budgetModule = module {
    viewModel {
        BudgetViewModel(
            budgetUseCase = get(),
            categoryUseCase = get(),
            getExpenseUseCase = get()
        )
    }
}