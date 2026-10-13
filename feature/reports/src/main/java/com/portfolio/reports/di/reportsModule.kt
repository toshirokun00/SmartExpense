package com.portfolio.reports.di

import com.portfolio.reports.viewmodel.ReportsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val expenseReportModule = module {
    viewModel {
        ReportsViewModel(getExpenseReportUseCase = get(),
            categoryUseCase = get())
    }
}