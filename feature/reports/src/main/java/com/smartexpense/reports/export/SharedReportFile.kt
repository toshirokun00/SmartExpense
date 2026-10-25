package com.smartexpense.reports.export

/**
 * A cache-backed export file that can be shared through a content URI.
 */
data class SharedReportFile(
    val uri: String,
    val mimeType: String
)
