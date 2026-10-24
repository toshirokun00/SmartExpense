package com.smartexpense.domain.export

import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.model.ExpenseReport

/**
 * The platform-agnostic content of a report export.
 *
 * CSV content is already formatted text. PDF content is a report model that a
 * platform-specific renderer can lay out without re-running report queries.
 */
sealed interface ReportExportContent {
    data class Csv(
        val text: String
    ) : ReportExportContent

    data class Pdf(
        val data: ReportPdfData
    ) : ReportExportContent
}

/**
 * Data required to render a transaction or summary PDF.
 *
 * [report] is populated for summary exports and [expenses] for transaction
 * exports. Category names are a snapshot so the PDF matches the report that
 * was visible when the export was requested.
 */
data class ReportPdfData(
    val type: ReportExportType,
    val startDateInclusive: Long,
    val endDateExclusive: Long,
    val categoryNames: Map<Long, String>,
    val report: ExpenseReport? = null,
    val expenses: List<Expense> = emptyList()
)
