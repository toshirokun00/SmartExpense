package com.smartexpense.reports.di

import com.smartexpense.reports.export.AndroidReportExportFileWriter
import com.smartexpense.reports.export.ReportExportFileWriter
import com.smartexpense.reports.viewmodel.ReportsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val expenseReportModule = module {
    single<ReportExportFileWriter> {
        AndroidReportExportFileWriter(context = androidContext())
    }

    viewModel {
        ReportsViewModel(
            getExpenseReportUseCase = get(),
            categoryUseCase = get(),
            exportReportCsvUseCase = get(),
            exportReportPdfUseCase = get(),
            reportExportFileWriter = get()
        )
    }
}
