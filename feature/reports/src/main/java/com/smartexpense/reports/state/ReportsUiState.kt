package com.smartexpense.reports.state

import com.smartexpense.domain.export.ReportExportFormat
import com.smartexpense.domain.export.ReportExportType
import com.smartexpense.domain.model.ExpenseReport

data class ReportsUiState(
    val report: ExpenseReport = ExpenseReport(),
    val categoryNames: Map<Long, String> = emptyMap(),
    val startDateInclusive: Long = 0L,
    val endDateExclusive: Long = 0L,
    val isStartDatePickerOpen: Boolean = false,
    val isEndDatePickerOpen: Boolean = false,
    val exportType: ReportExportType = ReportExportType.TRANSACTIONS,
    val exportFormat: ReportExportFormat = ReportExportFormat.CSV,
    val isExporting: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)
