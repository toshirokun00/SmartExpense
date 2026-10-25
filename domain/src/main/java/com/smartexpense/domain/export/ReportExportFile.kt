package com.smartexpense.domain.export

/**
 * A fully formatted export that is ready to be written or shared.
 *
 * [recordCount] is the number of exported transactions for transaction
 * exports, or the report expense count for summary exports. Headers are not
 * included in the count.
 */
data class ReportExportFile(
    val type: ReportExportType,
    val format: ReportExportFormat,
    val fileName: String,
    val content: ReportExportContent,
    val recordCount: Int
) {
    val isEmpty: Boolean
        get() = recordCount == 0
}
