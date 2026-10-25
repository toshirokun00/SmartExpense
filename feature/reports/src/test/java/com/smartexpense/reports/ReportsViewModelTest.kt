package com.smartexpense.reports

import app.cash.turbine.test
import com.smartexpense.domain.export.ReportExportContent
import com.smartexpense.domain.export.ReportExportFile
import com.smartexpense.domain.export.ReportExportFormat
import com.smartexpense.domain.export.ReportExportType
import com.smartexpense.domain.export.ReportPdfData
import com.smartexpense.reports.effect.ReportsEffect
import com.smartexpense.reports.export.ReportExportFileWriter
import com.smartexpense.reports.export.SharedReportFile
import com.smartexpense.reports.intent.ReportsIntent
import com.smartexpense.reports.viewmodel.ReportsViewModel
import com.smartexpense.domain.model.Category
import com.smartexpense.domain.model.CategorySpending
import com.smartexpense.domain.model.DailySpending
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.model.ExpenseReport
import com.smartexpense.domain.usecase.CategoryUseCase
import com.smartexpense.domain.usecase.ExportReportCsvUseCase
import com.smartexpense.domain.usecase.ExportReportPdfUseCase
import com.smartexpense.domain.usecase.GetExpenseReportUseCase
import com.smartexpense.test.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var getExpenseReportUseCase: GetExpenseReportUseCase
    private lateinit var categoryUseCase: CategoryUseCase
    private lateinit var exportReportCsvUseCase: ExportReportCsvUseCase
    private lateinit var exportReportPdfUseCase: ExportReportPdfUseCase
    private lateinit var fileWriter: FakeReportExportFileWriter

    private lateinit var viewModel: ReportsViewModel

    private val zoneId = ZoneId.systemDefault()

    @Before
    fun setUp() {
        getExpenseReportUseCase = mockk()
        categoryUseCase = mockk()
        exportReportCsvUseCase = mockk()
        exportReportPdfUseCase = mockk()
        fileWriter = FakeReportExportFileWriter()

        every {
            categoryUseCase.getCategories()
        } returns flowOf(
            listOf(
                Category(id = 1L, name = "Food"),
                Category(id = 2L, name = "Transport")
            )
        )

        every {
            getExpenseReportUseCase(any(), any())
        } returns flowOf(
            ExpenseReport(
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
                        date = timestamp(2026, 10, 5),
                        totalAmount = 300.0
                    )
                )
            )
        )

        coEvery {
            exportReportCsvUseCase(any(), any())
        } returns exportFile()

        coEvery {
            exportReportPdfUseCase(any(), any())
        } returns pdfExportFile()

        viewModel = createViewModel()
    }

    private fun createViewModel(): ReportsViewModel {
        return ReportsViewModel(
            getExpenseReportUseCase = getExpenseReportUseCase,
            categoryUseCase = categoryUseCase,
            exportReportCsvUseCase = exportReportCsvUseCase,
            exportReportPdfUseCase = exportReportPdfUseCase,
            reportExportFileWriter = fileWriter
        )
    }

    @Test
    fun initialLoadUpdatesReportAndCategoryNames() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertEquals(300.0, state.report.totalAmount, 0.001)
        assertEquals(2, state.report.expenseCount)
        assertEquals("Food", state.categoryNames[1L])
        assertEquals("Transport", state.categoryNames[2L])
    }

    @Test
    fun dateRangeChangedReloadsReport() = runTest {
        val start = timestamp(2026, 9, 1)
        val end = timestamp(2026, 10, 1)

        advanceUntilIdle()

        viewModel.onIntent(
            ReportsIntent.DateRangeChanged(
                startDateInclusive = start,
                endDateExclusive = end
            )
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(start, state.startDateInclusive)
        assertEquals(end, state.endDateExclusive)
        assertFalse(state.isLoading)
        assertEquals(300.0, state.report.totalAmount, 0.001)
    }

    @Test
    fun retryReloadsCurrentDateRange() = runTest {
        advanceUntilIdle()

        val originalStart = viewModel.uiState.value.startDateInclusive
        val originalEnd = viewModel.uiState.value.endDateExclusive

        viewModel.onIntent(ReportsIntent.Retry)
        advanceUntilIdle()

        assertEquals(originalStart, viewModel.uiState.value.startDateInclusive)
        assertEquals(originalEnd, viewModel.uiState.value.endDateExclusive)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun invalidDateRangeEmitsErrorEffect() = runTest {
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(
                ReportsIntent.DateRangeChanged(
                    startDateInclusive = timestamp(2026, 10, 10),
                    endDateExclusive = timestamp(2026, 10, 1)
                )
            )

            val effect = awaitItem()
            assertTrue(effect is ReportsEffect.ShowError)
            assertEquals(
                "End date must be after start date",
                (effect as ReportsEffect.ShowError).message
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun reportLoadingFailureUpdatesErrorState() = runTest {
        // This test uses a separate ViewModel so its initial report fails.
        every {
            getExpenseReportUseCase(any(), any())
        } returns flow {
            throw IllegalStateException("Unable to load report")
        }

        val failingViewModel = createViewModel()

        advanceUntilIdle()

        val state = failingViewModel.uiState.value

        assertFalse(state.isLoading)
        assertEquals("Unable to load report", state.error)
    }

    @Test
    fun pickerIntentsUpdatePickerState() = runTest {
        advanceUntilIdle()

        viewModel.onIntent(ReportsIntent.StartDatePickerOpened)
        assertTrue(viewModel.uiState.value.isStartDatePickerOpen)
        assertFalse(viewModel.uiState.value.isEndDatePickerOpen)

        viewModel.onIntent(ReportsIntent.EndDatePickerOpened)
        assertFalse(viewModel.uiState.value.isStartDatePickerOpen)
        assertTrue(viewModel.uiState.value.isEndDatePickerOpen)

        viewModel.onIntent(ReportsIntent.DatePickerDismissed)
        assertFalse(viewModel.uiState.value.isStartDatePickerOpen)
        assertFalse(viewModel.uiState.value.isEndDatePickerOpen)
    }

    @Test
    fun exportTypeSelectionIsStored() = runTest {
        advanceUntilIdle()

        viewModel.onIntent(
            ReportsIntent.ExportTypeSelected(ReportExportType.SUMMARY)
        )

        assertEquals(
            ReportExportType.SUMMARY,
            viewModel.uiState.value.exportType
        )
    }

    @Test
    fun exportFormatSelectionIsStored() = runTest {
        advanceUntilIdle()

        viewModel.onIntent(
            ReportsIntent.ExportFormatSelected(ReportExportFormat.PDF)
        )

        assertEquals(
            ReportExportFormat.PDF,
            viewModel.uiState.value.exportFormat
        )
    }

    @Test
    fun saveCsvUsesSelectedRangeAndTypeThenLaunchesDocument() = runTest {
        advanceUntilIdle()

        val expectedStart = viewModel.uiState.value.startDateInclusive
        val expectedEnd = viewModel.uiState.value.endDateExclusive

        viewModel.effect.test {
            viewModel.onIntent(ReportsIntent.SaveClicked)

            assertTrue(viewModel.uiState.value.isExporting)

            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is ReportsEffect.LaunchSaveDocument)
            effect as ReportsEffect.LaunchSaveDocument
            assertEquals(
                "smartexpense_transactions_2026-01-01_2026-01-31.csv",
                effect.fileName
            )
            assertEquals("text/csv", effect.mimeType)

            coVerify {
                exportReportCsvUseCase(
                    match {
                        it.type == ReportExportType.TRANSACTIONS &&
                            it.startDateInclusive == expectedStart &&
                            it.endDateExclusive == expectedEnd
                    },
                    match { it[1L] == "Food" }
                )
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun saveDestinationWritesCsvAndShowsSuccess() = runTest {
        advanceUntilIdle()

        coEvery {
            exportReportCsvUseCase(any(), any())
        } returns exportFile(content = "csv-content")

        viewModel.effect.test {
            viewModel.onIntent(ReportsIntent.SaveClicked)
            advanceUntilIdle()
            awaitItem()

            viewModel.onIntent(
                ReportsIntent.SaveDestinationSelected(
                    "content://downloads/report.csv"
                )
            )
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is ReportsEffect.ShowMessage)
            assertEquals(
                "CSV saved successfully",
                (effect as ReportsEffect.ShowMessage).message
            )

            cancelAndIgnoreRemainingEvents()
        }

        val write = fileWriter.writes.single()
        assertEquals("content://downloads/report.csv", write.first)
        assertEquals(ReportExportFormat.CSV, write.second.format)
        assertEquals(
            "csv-content",
            (write.second.content as ReportExportContent.Csv).text
        )
        assertFalse(viewModel.uiState.value.isExporting)
    }

    @Test
    fun emptyExportShowsMessageWithoutOpeningDestination() = runTest {
        advanceUntilIdle()

        coEvery {
            exportReportCsvUseCase(any(), any())
        } returns exportFile(recordCount = 0)

        viewModel.effect.test {
            viewModel.onIntent(ReportsIntent.SaveClicked)
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is ReportsEffect.ShowMessage)
            assertEquals(
                "No transactions found in the selected date range.",
                (effect as ReportsEffect.ShowMessage).message
            )

            cancelAndIgnoreRemainingEvents()
        }

        assertFalse(viewModel.uiState.value.isExporting)
        assertTrue(fileWriter.writes.isEmpty())
    }

    @Test
    fun shareExportCreatesSecureFileAndLaunchesShareEffect() = runTest {
        advanceUntilIdle()

        fileWriter.shareFile = SharedReportFile(
            uri = "content://com.smartexpense.fileprovider/report_exports/report.csv",
            mimeType = "text/csv"
        )

        viewModel.effect.test {
            viewModel.onIntent(ReportsIntent.ShareClicked)
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is ReportsEffect.LaunchShare)
            effect as ReportsEffect.LaunchShare
            assertEquals(fileWriter.shareFile.uri, effect.fileUri)
            assertEquals("text/csv", effect.mimeType)
            assertEquals("Share SmartExpense CSV", effect.title)

            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(1, fileWriter.shareRequests.size)
        assertEquals(
            ReportExportFormat.CSV,
            fileWriter.shareRequests.single().format
        )
        assertFalse(viewModel.uiState.value.isExporting)
    }

    @Test
    fun savePdfUsesPdfUseCaseAndLaunchesPdfDocument() = runTest {
        advanceUntilIdle()

        viewModel.onIntent(
            ReportsIntent.ExportFormatSelected(ReportExportFormat.PDF)
        )

        viewModel.effect.test {
            viewModel.onIntent(ReportsIntent.SaveClicked)
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is ReportsEffect.LaunchSaveDocument)
            effect as ReportsEffect.LaunchSaveDocument
            assertEquals(
                "smartexpense_transactions_2026-01-01_2026-01-31.pdf",
                effect.fileName
            )
            assertEquals("application/pdf", effect.mimeType)

            coVerify {
                exportReportPdfUseCase(any(), any())
            }
            coVerify(exactly = 0) {
                exportReportCsvUseCase(any(), any())
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun saveDestinationWritesPdfAndShowsSuccess() = runTest {
        advanceUntilIdle()

        coEvery {
            exportReportPdfUseCase(any(), any())
        } returns pdfExportFile()

        viewModel.onIntent(
            ReportsIntent.ExportFormatSelected(ReportExportFormat.PDF)
        )

        viewModel.effect.test {
            viewModel.onIntent(ReportsIntent.SaveClicked)
            advanceUntilIdle()
            awaitItem()

            viewModel.onIntent(
                ReportsIntent.SaveDestinationSelected(
                    "content://downloads/report.pdf"
                )
            )
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is ReportsEffect.ShowMessage)
            assertEquals(
                "PDF saved successfully",
                (effect as ReportsEffect.ShowMessage).message
            )

            cancelAndIgnoreRemainingEvents()
        }

        val write = fileWriter.writes.single()
        assertEquals("content://downloads/report.pdf", write.first)
        assertEquals(ReportExportFormat.PDF, write.second.format)
        assertTrue(write.second.content is ReportExportContent.Pdf)
        assertFalse(viewModel.uiState.value.isExporting)
    }

    @Test
    fun sharePdfCreatesPdfShareFileAndLaunchesPdfShareEffect() = runTest {
        advanceUntilIdle()

        coEvery {
            exportReportPdfUseCase(any(), any())
        } returns pdfExportFile()

        fileWriter.shareFile = SharedReportFile(
            uri = "content://com.smartexpense.fileprovider/report_exports/report.pdf",
            mimeType = "application/pdf"
        )

        viewModel.onIntent(
            ReportsIntent.ExportFormatSelected(ReportExportFormat.PDF)
        )

        viewModel.effect.test {
            viewModel.onIntent(ReportsIntent.ShareClicked)
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is ReportsEffect.LaunchShare)
            effect as ReportsEffect.LaunchShare
            assertEquals(fileWriter.shareFile.uri, effect.fileUri)
            assertEquals("application/pdf", effect.mimeType)
            assertEquals("Share SmartExpense PDF", effect.title)

            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(
            ReportExportFormat.PDF,
            fileWriter.shareRequests.single().format
        )
        assertFalse(viewModel.uiState.value.isExporting)
    }

    @Test
    fun exportFailureShowsErrorAndResetsState() = runTest {
        advanceUntilIdle()

        coEvery {
            exportReportCsvUseCase(any(), any())
        } throws IllegalStateException("Export failed")

        viewModel.effect.test {
            viewModel.onIntent(ReportsIntent.SaveClicked)
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is ReportsEffect.ShowError)
            assertEquals(
                "Export failed",
                (effect as ReportsEffect.ShowError).message
            )

            cancelAndIgnoreRemainingEvents()
        }

        assertFalse(viewModel.uiState.value.isExporting)
    }

    @Test
    fun shareFileCreationFailureShowsErrorAndResetsState() = runTest {
        advanceUntilIdle()

        fileWriter.shareException = IllegalStateException("Share failed")

        viewModel.effect.test {
            viewModel.onIntent(ReportsIntent.ShareClicked)
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is ReportsEffect.ShowError)
            assertEquals(
                "Share failed",
                (effect as ReportsEffect.ShowError).message
            )

            cancelAndIgnoreRemainingEvents()
        }

        assertFalse(viewModel.uiState.value.isExporting)
    }

    @Test
    fun saveWriteFailureShowsErrorAndResetsState() = runTest {
        advanceUntilIdle()

        fileWriter.writeException = IllegalStateException("Disk full")

        viewModel.effect.test {
            viewModel.onIntent(ReportsIntent.SaveClicked)
            advanceUntilIdle()
            awaitItem()

            viewModel.onIntent(
                ReportsIntent.SaveDestinationSelected(
                    "content://downloads/report.csv"
                )
            )
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is ReportsEffect.ShowError)
            assertEquals(
                "Disk full",
                (effect as ReportsEffect.ShowError).message
            )

            cancelAndIgnoreRemainingEvents()
        }

        assertFalse(viewModel.uiState.value.isExporting)
    }

    @Test
    fun saveCancellationResetsStateWithoutWriting() = runTest {
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(ReportsIntent.SaveClicked)
            advanceUntilIdle()
            awaitItem()

            viewModel.onIntent(
                ReportsIntent.SaveDestinationSelectionCancelled
            )
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is ReportsEffect.ShowMessage)
            assertEquals(
                "Export cancelled",
                (effect as ReportsEffect.ShowMessage).message
            )

            cancelAndIgnoreRemainingEvents()
        }

        assertFalse(viewModel.uiState.value.isExporting)
        assertTrue(fileWriter.writes.isEmpty())
    }

    @Test
    fun duplicateExportIntentIsIgnoredWhileSaveIsPending() = runTest {
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(ReportsIntent.SaveClicked)
            advanceUntilIdle()
            awaitItem()

            viewModel.onIntent(ReportsIntent.ShareClicked)
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is ReportsEffect.ShowMessage)
            assertEquals(
                "An export is already in progress",
                (effect as ReportsEffect.ShowMessage).message
            )

            coVerify(exactly = 1) {
                exportReportCsvUseCase(any(), any())
            }
            assertTrue(fileWriter.shareRequests.isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun shareLaunchFailureShowsError() = runTest {
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(
                ReportsIntent.ExportLaunchFailed(
                    "Could not open the share sheet"
                )
            )
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is ReportsEffect.ShowError)
            assertEquals(
                "Could not open the share sheet",
                (effect as ReportsEffect.ShowError).message
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun exportFile(
        content: String = "csv-content",
        recordCount: Int = 2
    ): ReportExportFile {
        return ReportExportFile(
            type = ReportExportType.TRANSACTIONS,
            format = ReportExportFormat.CSV,
            fileName = "smartexpense_transactions_2026-01-01_2026-01-31.csv",
            content = ReportExportContent.Csv(content),
            recordCount = recordCount
        )
    }

    private fun pdfExportFile(
        recordCount: Int = 2
    ): ReportExportFile {
        return ReportExportFile(
            type = ReportExportType.TRANSACTIONS,
            format = ReportExportFormat.PDF,
            fileName = "smartexpense_transactions_2026-01-01_2026-01-31.pdf",
            content = ReportExportContent.Pdf(
                ReportPdfData(
                    type = ReportExportType.TRANSACTIONS,
                    startDateInclusive = timestamp(2026, 1, 1),
                    endDateExclusive = timestamp(2026, 2, 1),
                    categoryNames = mapOf(1L to "Food"),
                    expenses = listOf(
                        Expense(
                            id = 1L,
                            amount = 10.0,
                            categoryId = 1L,
                            description = "Lunch",
                            date = timestamp(2026, 1, 15)
                        )
                    )
                )
            ),
            recordCount = recordCount
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

private class FakeReportExportFileWriter : ReportExportFileWriter {

    val writes = mutableListOf<Pair<String, ReportExportFile>>()
    val shareRequests = mutableListOf<ReportExportFile>()

    var shareFile = SharedReportFile(
        uri = "content://share/report.csv",
        mimeType = "text/csv"
    )
    var writeException: Exception? = null
    var shareException: Exception? = null

    override suspend fun writeToDocument(
        uri: String,
        export: ReportExportFile
    ) {
        writeException?.let { throw it }
        writes += uri to export
    }

    override suspend fun createShareFile(
        export: ReportExportFile
    ): SharedReportFile {
        shareException?.let { throw it }
        shareRequests += export
        return shareFile
    }
}