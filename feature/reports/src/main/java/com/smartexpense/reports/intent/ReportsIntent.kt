package com.smartexpense.reports.intent

import com.smartexpense.domain.export.ReportExportFormat
import com.smartexpense.domain.export.ReportExportType

sealed interface ReportsIntent {
    data class DateRangeChanged(
        val startDateInclusive: Long,
        val endDateExclusive: Long
    ) : ReportsIntent

    data object StartDatePickerOpened : ReportsIntent
    data object EndDatePickerOpened : ReportsIntent
    data object DatePickerDismissed : ReportsIntent

    data class ExportTypeSelected(
        val exportType: ReportExportType
    ) : ReportsIntent

    data class ExportFormatSelected(
        val exportFormat: ReportExportFormat
    ) : ReportsIntent

    data object SaveClicked : ReportsIntent
    data object ShareClicked : ReportsIntent

    data class SaveDestinationSelected(
        val uri: String
    ) : ReportsIntent

    data object SaveDestinationSelectionCancelled : ReportsIntent

    data class ExportLaunchFailed(
        val message: String
    ) : ReportsIntent

    data object Retry : ReportsIntent
}
