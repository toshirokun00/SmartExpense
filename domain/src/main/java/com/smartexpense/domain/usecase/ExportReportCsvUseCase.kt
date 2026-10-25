package com.smartexpense.domain.usecase

import com.smartexpense.domain.export.ReportCsvFormatter
import com.smartexpense.domain.export.ReportExportContent
import com.smartexpense.domain.export.ReportExportFile
import com.smartexpense.domain.export.ReportExportFormat
import com.smartexpense.domain.export.ReportExportFormatting
import com.smartexpense.domain.export.ReportExportRequest
import com.smartexpense.domain.export.ReportExportType
import kotlinx.coroutines.flow.first
import java.time.ZoneId

/**
 * Builds either a transaction-detail CSV or a report-summary CSV from the
 * local expense source.
 *
 * Summary exports use [GetExpenseReportUseCase] directly so existing totals,
 * category percentages, daily grouping, and date filtering remain the single
 * source of truth. Transaction exports use [GetExpensesInRangeUseCase] with
 * the same date boundaries.
 */
class ExportReportCsvUseCase(
    private val getExpenseReportUseCase: GetExpenseReportUseCase,
    private val getExpensesInRangeUseCase: GetExpensesInRangeUseCase,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    private val formatter = ReportCsvFormatter(zoneId)

    suspend operator fun invoke(
        request: ReportExportRequest,
        categoryNames: Map<Long, String>
    ): ReportExportFile {
        require(request.endDateExclusive > request.startDateInclusive) {
            "End date must be after start date"
        }

        return when (request.type) {
            ReportExportType.TRANSACTIONS -> {
                val expenses = getExpensesInRangeUseCase(
                    startDateInclusive = request.startDateInclusive,
                    endDateExclusive = request.endDateExclusive
                ).first()

                ReportExportFile(
                    type = request.type,
                    format = ReportExportFormat.CSV,
                    fileName = fileName(
                        prefix = "smartexpense_transactions",
                        request = request
                    ),
                    content = ReportExportContent.Csv(
                        formatter.transactions(
                            expenses = expenses,
                            categoryNames = categoryNames
                        )
                    ),
                    recordCount = expenses.size
                )
            }

            ReportExportType.SUMMARY -> {
                val report = getExpenseReportUseCase(
                    startDateInclusive = request.startDateInclusive,
                    endDateExclusive = request.endDateExclusive
                ).first()

                ReportExportFile(
                    type = request.type,
                    format = ReportExportFormat.CSV,
                    fileName = fileName(
                        prefix = "smartexpense_report",
                        request = request
                    ),
                    content = ReportExportContent.Csv(
                        formatter.summary(
                            report = report,
                            categoryNames = categoryNames,
                            startDateInclusive = request.startDateInclusive,
                            endDateExclusive = request.endDateExclusive
                        )
                    ),
                    recordCount = report.expenseCount
                )
            }
        }
    }

    private fun fileName(
        prefix: String,
        request: ReportExportRequest
    ): String {
        return ReportExportFormatting.fileName(
            prefix = prefix,
            startDateInclusive = request.startDateInclusive,
            endDateExclusive = request.endDateExclusive,
            format = ReportExportFormat.CSV,
            zoneId = zoneId
        )
    }
}
