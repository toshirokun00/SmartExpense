package com.smartexpense.domain.usecase

import com.smartexpense.domain.export.ReportExportContent
import com.smartexpense.domain.export.ReportExportFile
import com.smartexpense.domain.export.ReportExportFormat
import com.smartexpense.domain.export.ReportExportFormatting
import com.smartexpense.domain.export.ReportExportRequest
import com.smartexpense.domain.export.ReportExportType
import com.smartexpense.domain.export.ReportPdfData
import com.smartexpense.domain.model.Expense
import kotlinx.coroutines.flow.first
import java.time.ZoneId

/**
 * Builds platform-agnostic PDF export data from the local expense source.
 *
 * The Android feature layer is responsible for laying this data out with
 * `android.graphics.pdf.PdfDocument`. Report totals and groupings come from
 * [GetExpenseReportUseCase], so PDF summaries stay consistent with the
 * on-screen report and CSV export.
 */
class ExportReportPdfUseCase(
    private val getExpenseReportUseCase: GetExpenseReportUseCase,
    private val getExpensesInRangeUseCase: GetExpensesInRangeUseCase,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    suspend operator fun invoke(
        request: ReportExportRequest,
        categoryNames: Map<Long, String>
    ): ReportExportFile {
        require(request.endDateExclusive > request.startDateInclusive) {
            "End date must be after start date"
        }

        val pdfData = when (request.type) {
            ReportExportType.TRANSACTIONS -> {
                val expenses = getExpensesInRangeUseCase(
                    startDateInclusive = request.startDateInclusive,
                    endDateExclusive = request.endDateExclusive
                ).first()
                    .sortedWith(compareBy<Expense> { it.date }.thenBy { it.id })

                ReportPdfData(
                    type = request.type,
                    startDateInclusive = request.startDateInclusive,
                    endDateExclusive = request.endDateExclusive,
                    categoryNames = categoryNames.toMap(),
                    expenses = expenses
                )
            }

            ReportExportType.SUMMARY -> {
                val report = getExpenseReportUseCase(
                    startDateInclusive = request.startDateInclusive,
                    endDateExclusive = request.endDateExclusive
                ).first()

                ReportPdfData(
                    type = request.type,
                    startDateInclusive = request.startDateInclusive,
                    endDateExclusive = request.endDateExclusive,
                    categoryNames = categoryNames.toMap(),
                    report = report
                )
            }
        }

        return ReportExportFile(
            type = request.type,
            format = ReportExportFormat.PDF,
            fileName = ReportExportFormatting.fileName(
                prefix = when (request.type) {
                    ReportExportType.TRANSACTIONS ->
                        "smartexpense_transactions"

                    ReportExportType.SUMMARY -> "smartexpense_report"
                },
                startDateInclusive = request.startDateInclusive,
                endDateExclusive = request.endDateExclusive,
                format = ReportExportFormat.PDF,
                zoneId = zoneId
            ),
            content = ReportExportContent.Pdf(pdfData),
            recordCount = pdfData.report?.expenseCount ?: pdfData.expenses.size
        )
    }
}
