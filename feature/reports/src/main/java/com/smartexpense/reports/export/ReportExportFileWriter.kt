package com.smartexpense.reports.export

import com.smartexpense.domain.export.ReportExportFile

/**
 * Platform file operations for report exports.
 *
 * Implementations must not expose filesystem paths to other applications;
 * shared files are exposed through a restricted content URI.
 */
interface ReportExportFileWriter {

    /**
     * Writes [export] to a document URI returned by the Storage Access
     * Framework.
     */
    suspend fun writeToDocument(
        uri: String,
        export: ReportExportFile
    )

    /**
     * Writes [export] to a private cache file and returns its shareable
     * content URI. Previous temporary exports are removed first.
     */
    suspend fun createShareFile(
        export: ReportExportFile
    ): SharedReportFile
}
