package com.smartexpense.reports.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.smartexpense.domain.export.ReportExportContent
import com.smartexpense.domain.export.ReportExportFile
import com.smartexpense.domain.export.ReportExportFormat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.OutputStream

/**
 * Android implementation of report export file operations.
 *
 * SAF destinations are written directly through [android.content.ContentResolver].
 * Share files live in a private cache directory and are exposed only through the
 * app's restricted FileProvider.
 */
class AndroidReportExportFileWriter(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ReportExportFileWriter {

    private val pdfRenderer = ReportPdfRenderer()

    override suspend fun writeToDocument(
        uri: String,
        export: ReportExportFile
    ) {
        withContext(ioDispatcher) {
            val destination = Uri.parse(uri)
            val outputStream = context.contentResolver.openOutputStream(
                destination,
                WRITE_MODE
            ) ?: throw IOException("Could not open the selected destination")

            outputStream.use {
                writeExport(export, it)
            }
        }
    }

    override suspend fun createShareFile(
        export: ReportExportFile
    ): SharedReportFile = withContext(ioDispatcher) {
        val exportDirectory = exportDirectory()
        removeTemporaryExports(exportDirectory)

        val safeFileName = export.fileName
            .substringAfterLast('/')
            .substringAfterLast('\\')

        val expectedExtension = ".${export.format.fileExtension}"

        require(
            safeFileName.isNotBlank() &&
                safeFileName == export.fileName &&
                safeFileName.endsWith(expectedExtension, ignoreCase = true)
        ) {
            "Invalid export file name"
        }

        val file = File(exportDirectory, safeFileName)
        file.outputStream().use { outputStream ->
            writeExport(export, outputStream)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}$FILE_PROVIDER_SUFFIX",
            file
        )

        SharedReportFile(
            uri = uri.toString(),
            mimeType = export.format.mimeType
        )
    }

    private fun writeExport(
        export: ReportExportFile,
        outputStream: OutputStream
    ) {
        when (val content = export.content) {
            is ReportExportContent.Csv -> {
                outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(content.text)
                }
            }

            is ReportExportContent.Pdf -> {
                pdfRenderer.write(export, outputStream)
            }
        }
    }

    private fun exportDirectory(): File {
        val directory = File(context.cacheDir, EXPORT_DIRECTORY)

        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Could not create the export cache directory")
        }

        if (!directory.isDirectory) {
            throw IOException("Export cache path is not a directory")
        }

        return directory
    }

    private fun removeTemporaryExports(directory: File) {
        directory.listFiles()
            ?.filter(File::isFile)
            ?.forEach { file ->
                // A failed cleanup must not prevent a new export. The OS also
                // clears cache data when storage is needed.
                file.delete()
            }
    }

    companion object {
        const val FILE_PROVIDER_SUFFIX = ".fileprovider"
        const val EXPORT_DIRECTORY = "report_exports"
        private const val WRITE_MODE = "wt"
    }
}
