package com.smartexpense.paycheck.di

import com.smartexpense.paycheck.viewmodel.MoneyTrackerViewModel
import com.smartexpense.paycheck.viewmodel.PaycheckFormViewModel
import com.smartexpense.paycheck.viewmodel.PaychecksViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val paycheckModule = module {
    viewModel {
        PaychecksViewModel(
            getPaychecksUseCase = get(),
            getBillsUseCase = get(),
            getBillPaymentHistoryUseCase = get(),
            clock = get()
        )
    }

    viewModel {
        MoneyTrackerViewModel(
            getPaychecksUseCase = get(),
            getBillsUseCase = get(),
            getExpenseUseCase = get(),
            clock = get()
        )
    }

    viewModel {
        PaycheckFormViewModel(
            getPaycheckUseCase = get(),
            addPaycheckUseCase = get(),
            updatePaycheckUseCase = get(),
            deletePaycheckUseCase = get(),
            addPaycheckScheduleUseCase = get(),
            stopPaycheckScheduleUseCase = get(),
            getBillsUseCase = get(),
            getPaychecksUseCase = get(),
            getBillPaymentHistoryUseCase = get(),
            clock = get()
        )
    }
}
