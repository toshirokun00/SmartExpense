package com.smartexpense.domain.export

/**
 * The supported report export formats.
 *
 * Transaction details and report summaries have different table structures,
 * so each type is written to its own CSV file.
 */
enum class ReportExportType {
    TRANSACTIONS,
    SUMMARY
}
