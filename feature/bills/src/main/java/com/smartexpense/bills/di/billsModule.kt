package com.smartexpense.bills.di

import com.smartexpense.bills.viewmodel.BillFormViewModel
import com.smartexpense.bills.viewmodel.BillHistoryViewModel
import com.smartexpense.bills.viewmodel.BillsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val billsModule = module {
    viewModel {
        BillsViewModel(
            getBillsUseCase = get(),
            payBillUseCase = get(),
            categoryUseCase = get()
        )
    }

    viewModel {
        BillFormViewModel(
            getBillUseCase = get(),
            addBillUseCase = get(),
            updateBillUseCase = get(),
            deleteBillUseCase = get(),
            skipBillUseCase = get(),
            categoryUseCase = get()
        )
    }

    viewModel {
        BillHistoryViewModel(
            getBillPaymentHistoryUseCase = get(),
            clock = get()
        )
    }
}
