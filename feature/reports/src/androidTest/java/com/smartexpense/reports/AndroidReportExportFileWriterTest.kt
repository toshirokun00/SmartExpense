package com.smartexpense.reports

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartexpense.domain.export.ReportExportContent
import com.smartexpense.domain.export.ReportExportFile
import com.smartexpense.domain.export.ReportExportFormat
import com.smartexpense.domain.export.ReportExportType
import com.smartexpense.domain.export.ReportPdfData
import com.smartexpense.domain.model.CategorySpending
import com.smartexpense.domain.model.DailySpending
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.model.ExpenseReport
import com.smartexpense.reports.export.AndroidReportExportFileWriter
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileNotFoundException
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class AndroidReportExportFileWriterTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val writer = AndroidReportExportFileWriter(context)
    private val zoneId = ZoneId.of("UTC")

    @After
    fun tearDown() {
        File(
            context.cacheDir,
            AndroidReportExportFileWriter.EXPORT_DIRECTORY
        ).deleteRecursively()
    }

    @Test
    fun createShareFileWritesCsvContentAndReturnsReadableContentUri() = runBlocking {
        val shared = writer.createShareFile(
            export = csvExport(
                fileName = "smartexpense_transactions_2026-01-01_2026-01-31.csv",
                content = "a,b\r\n1,2"
            )
        )

        val uri = Uri.parse(shared.uri)
        assertEquals("content", uri.scheme)
        assertEquals("text/csv", shared.mimeType)
        assertTrue(
            shared.uri.contains(AndroidReportExportFileWriter.EXPORT_DIRECTORY)
        )

        val content = context.contentResolver.openInputStream(uri)
            ?.bufferedReader()
            ?.use { it.readText() }

        assertEquals("a,b\r\n1,2", content)
    }

    @Test
    fun createShareFileRemovesPreviousTemporaryExports() = runBlocking {
        val first = writer.createShareFile(
            export = csvExport(
                fileName = "smartexpense_transactions_2026-01-01_2026-01-31.csv",
                content = "first"
            )
        )
        val second = writer.createShareFile(
            export = csvExport(
                fileName = "smartexpense_report_2026-01-01_2026-01-31.csv",
                content = "second"
            )
        )

        assertThrows(FileNotFoundException::class.java) {
            context.contentResolver.openInputStream(Uri.parse(first.uri))
        }

        val secondContent = context.contentResolver
            .openInputStream(Uri.parse(second.uri))
            ?.bufferedReader()
            ?.use { it.readText() }

        assertEquals("second", secondContent)
    }

    @Test
    fun createShareFileWritesRenderableTransactionPdf() = runBlocking {
        val shared = writer.createShareFile(
            export = transactionPdfExport()
        )

        assertEquals("application/pdf", shared.mimeType)
        assertPdfHasReadablePages(Uri.parse(shared.uri))
    }

    @Test
    fun createShareFileWritesRenderableSummaryPdf() = runBlocking {
        val shared = writer.createShareFile(
            export = summaryPdfExport()
        )

        assertEquals("application/pdf", shared.mimeType)
        assertPdfHasReadablePages(Uri.parse(shared.uri))
    }

    @Test
    fun writeToDocumentWritesCsvContent() = runBlocking {
        val file = File(context.cacheDir, "saf_export_test.csv")

        try {
            writer.writeToDocument(
                uri = Uri.fromFile(file).toString(),
                export = csvExport(
                    fileName = "smartexpense_transactions_2026-01-01_2026-01-31.csv",
                    content = "header\r\nvalue"
                )
            )

            assertEquals("header\r\nvalue", file.readText())
        } finally {
            file.delete()
        }
    }

    @Test
    fun writeToDocumentWritesRenderablePdf() = runBlocking {
        val file = File(context.cacheDir, "saf_export_test.pdf")

        try {
            writer.writeToDocument(
                uri = Uri.fromFile(file).toString(),
                export = transactionPdfExport()
            )

            assertTrue(file.length() > 100L)
            assertEquals(
                "%PDF-",
                file.readBytes()
                    .copyOfRange(0, 5)
                    .toString(Charsets.US_ASCII)
            )
        } finally {
            file.delete()
        }
    }

    @Test
    fun createShareFileRejectsAFormatMismatchedFileName() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                writer.createShareFile(
                    export = csvExport(
                        fileName = "smartexpense_report_2026-01-01_2026-01-31.pdf"
                    )
                )
            }
        }
    }

    @Test
    fun writeToDocumentFailsForAnUnavailableDestination() {
        assertThrows(Exception::class.java) {
            runBlocking {
                writer.writeToDocument(
                    uri = "content://missing.provider/report.csv",
                    export = csvExport()
                )
            }
        }
    }

    private fun assertPdfHasReadablePages(uri: Uri) {
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: error("Could not open the shared PDF")

        try {
            PdfRenderer(descriptor).use { renderer ->
                assertTrue(renderer.pageCount > 0)

                renderer.openPage(0).use { page ->
                    assertEquals(595, page.width)
                    assertEquals(842, page.height)
                }
            }
        } finally {
            descriptor.close()
        }
    }

    private fun csvExport(
        fileName: String = "smartexpense_transactions_2026-01-01_2026-01-31.csv",
        content: String = "a,b\r\n1,2",
        recordCount: Int = 1
    ): ReportExportFile {
        return ReportExportFile(
            type = ReportExportType.TRANSACTIONS,
            format = ReportExportFormat.CSV,
            fileName = fileName,
            content = ReportExportContent.Csv(content),
            recordCount = recordCount
        )
    }

    private fun transactionPdfExport(): ReportExportFile {
        return ReportExportFile(
            type = ReportExportType.TRANSACTIONS,
            format = ReportExportFormat.PDF,
            fileName = "smartexpense_transactions_2026-01-01_2026-01-31.pdf",
            content = ReportExportContent.Pdf(
                ReportPdfData(
                    type = ReportExportType.TRANSACTIONS,
                    startDateInclusive = timestamp(2026, 1, 1),
                    endDateExclusive = timestamp(2026, 2, 1),
                    categoryNames = mapOf(1L to "Food", 2L to "Transport"),
                    expenses = listOf(
                        Expense(
                            id = 1L,
                            amount = 12.3,
                            categoryId = 1L,
                            description = "Lunch",
                            date = timestamp(2026, 1, 15)
                        ),
                        Expense(
                            id = 2L,
                            amount = 45.0,
                            categoryId = 2L,
                            description = "Train fare",
                            date = timestamp(2026, 1, 16)
                        )
                    )
                )
            ),
            recordCount = 2
        )
    }

    private fun summaryPdfExport(): ReportExportFile {
        return ReportExportFile(
            type = ReportExportType.SUMMARY,
            format = ReportExportFormat.PDF,
            fileName = "smartexpense_report_2026-01-01_2026-01-31.pdf",
            content = ReportExportContent.Pdf(
                ReportPdfData(
                    type = ReportExportType.SUMMARY,
                    startDateInclusive = timestamp(2026, 1, 1),
                    endDateExclusive = timestamp(2026, 2, 1),
                    categoryNames = mapOf(1L to "Food", 2L to "Transport"),
                    report = ExpenseReport(
                        totalAmount = 300.0,
                        expenseCount = 2,
                        averageAmount = 150.0,
                        categoryBreakdown = listOf(
                            CategorySpending(
                                categoryId = 1L,
                                totalAmount = 200.0,
                                percentage = 66.67
                            ),
                            CategorySpending(
                                categoryId = 2L,
                                totalAmount = 100.0,
                                percentage = 33.33
                            )
                        ),
                        dailySpending = listOf(
                            DailySpending(
                                date = timestamp(2026, 1, 5),
                                totalAmount = 300.0
                            )
                        )
                    )
                )
            ),
            recordCount = 2
        )
    }

    private fun timestamp(
        year: Int,
        month: Int,
        day: Int
    ): Long {
        return LocalDate.of(year, month, day)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }
}
