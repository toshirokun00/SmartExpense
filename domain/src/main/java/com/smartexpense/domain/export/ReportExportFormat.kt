package com.smartexpense.domain.export

/**
 * The supported report export formats.
 */
enum class ReportExportFormat(
    val fileExtension: String
) {
    CSV("csv"),
    PDF("pdf")
}
