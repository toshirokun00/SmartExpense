package com.portfolio.reports

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val expenseReportModule = module {
    viewModel {
        ReportsViewModel(getExpenseReportUseCase = get())
    }
}