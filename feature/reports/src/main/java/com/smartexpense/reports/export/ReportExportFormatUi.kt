package com.smartexpense.reports.export

import com.smartexpense.domain.export.ReportExportFormat

/**
 * Presentation values for the supported export formats.
 */
val ReportExportFormat.mimeType: String
    get() = when (this) {
        ReportExportFormat.CSV -> "text/csv"
        ReportExportFormat.PDF -> "application/pdf"
    }

val ReportExportFormat.displayName: String
    get() = when (this) {
        ReportExportFormat.CSV -> "CSV"
        ReportExportFormat.PDF -> "PDF"
    }
