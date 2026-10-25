package com.smartexpense.domain.export

/**
 * A request to export report data for one date range.
 *
 * [endDateExclusive] follows the Reports screen convention: the user-selected
 * inclusive end date is converted to the start of the following day before it
 * reaches this request.
 */
data class ReportExportRequest(
    val type: ReportExportType,
    val startDateInclusive: Long,
    val endDateExclusive: Long
)
