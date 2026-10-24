package com.smartexpense.domain

import com.smartexpense.domain.export.ReportExportContent
import com.smartexpense.domain.export.ReportExportFile
import com.smartexpense.domain.export.ReportExportFormat
import com.smartexpense.domain.export.ReportExportRequest
import com.smartexpense.domain.export.ReportExportType
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.repository.ExpenseRepository
import com.smartexpense.domain.usecase.ExportReportCsvUseCase
import com.smartexpense.domain.usecase.GetExpenseReportUseCase
import com.smartexpense.domain.usecase.GetExpensesInRangeUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ExportReportCsvUseCaseTest {

    private lateinit var repository: ExpenseRepository
    private lateinit var useCase: ExportReportCsvUseCase

    private val zoneId = ZoneId.of("UTC")

    @Before
    fun setUp() {
        repository = mockk()
        useCase = ExportReportCsvUseCase(
            getExpenseReportUseCase = GetExpenseReportUseCase(repository),
            getExpensesInRangeUseCase = GetExpensesInRangeUseCase(repository),
            zoneId = zoneId
        )
    }

    @Test
    fun transactionExportUsesInclusiveStartAndExclusiveEnd() = runTest {
        every { repository.getExpense() } returns flowOf(
            listOf(
                expense(1L, 10.0, 2026, 1, 1),
                expense(2L, 20.0, 2026, 1, 31),
                expense(3L, 30.0, 2026, 2, 1),
                expense(4L, 40.0, 2025, 12, 31)
            )
        )

        val result = useCase(
            request = request(
                type = ReportExportType.TRANSACTIONS,
                start = timestamp(2026, 1, 1),
                end = timestamp(2026, 2, 1)
            ),
            categoryNames = mapOf(1L to "Food")
        )

        assertEquals(ReportExportType.TRANSACTIONS, result.type)
        assertEquals(ReportExportFormat.CSV, result.format)
        assertEquals(
            "smartexpense_transactions_2026-01-01_2026-01-31.csv",
            result.fileName
        )
        assertEquals(2, result.recordCount)
        assertFalse(result.isEmpty)
        assertTrue(result.csvText().contains("1,2026-01-01,Food,Lunch,10.00,PHP"))
        assertTrue(result.csvText().contains("2,2026-01-31,Food,Lunch,20.00,PHP"))
        assertFalse(result.csvText().contains("3,2026-02-01"))
        assertFalse(result.csvText().contains("4,2025-12-31"))
    }

    @Test
    fun summaryExportReusesExistingReportCalculations() = runTest {
        every { repository.getExpense() } returns flowOf(
            listOf(
                expense(1L, 200.0, 2026, 1, 5, categoryId = 1L),
                expense(2L, 100.0, 2026, 1, 5, categoryId = 2L)
            )
        )

        val result = useCase(
            request = request(
                type = ReportExportType.SUMMARY,
                start = timestamp(2026, 1, 1),
                end = timestamp(2026, 2, 1)
            ),
            categoryNames = mapOf(1L to "Food", 2L to "Transport")
        )

        assertEquals(ReportExportType.SUMMARY, result.type)
        assertEquals(ReportExportFormat.CSV, result.format)
        assertEquals(
            "smartexpense_report_2026-01-01_2026-01-31.csv",
            result.fileName
        )
        assertEquals(2, result.recordCount)
        assertTrue(result.csvText().contains("summary,Total Expenses,300.00,,"))
        assertTrue(result.csvText().contains("summary,Transaction Count,2,,"))
        assertTrue(result.csvText().contains("category,Food,200.00,66.67,"))
        assertTrue(result.csvText().contains("category,Transport,100.00,33.33,"))
        assertTrue(result.csvText().contains("daily,Daily Total,300.00,,"))
    }

    @Test
    fun emptyTransactionExportHasHeadersAndIsMarkedEmpty() = runTest {
        every { repository.getExpense() } returns flowOf(emptyList())

        val result = useCase(
            request = request(
                type = ReportExportType.TRANSACTIONS,
                start = timestamp(2026, 1, 1),
                end = timestamp(2026, 2, 1)
            ),
            categoryNames = emptyMap()
        )

        assertEquals(0, result.recordCount)
        assertTrue(result.isEmpty)
        assertEquals(
            "transaction_id,expense_date,category_name,description,amount,currency",
            result.csvText()
        )
    }

    @Test
    fun invalidDateRangeIsRejectedExplicitly() = runTest {
        val exception = runCatching {
            useCase(
                request = request(
                    type = ReportExportType.TRANSACTIONS,
                    start = timestamp(2026, 1, 31),
                    end = timestamp(2026, 1, 1)
                ),
                categoryNames = emptyMap()
            )
        }.exceptionOrNull()

        assertTrue(exception is IllegalArgumentException)
        assertEquals(
            "End date must be after start date",
            exception?.message
        )
    }

    private fun ReportExportFile.csvText(): String {
        return (content as ReportExportContent.Csv).text
    }

    private fun request(
        type: ReportExportType,
        start: Long,
        end: Long
    ): ReportExportRequest {
        return ReportExportRequest(
            type = type,
            startDateInclusive = start,
            endDateExclusive = end
        )
    }

    private fun expense(
        id: Long,
        amount: Double,
        year: Int,
        month: Int,
        day: Int,
        categoryId: Long = 1L
    ): Expense {
        return Expense(
            id = id,
            amount = amount,
            categoryId = categoryId,
            description = "Lunch",
            date = timestamp(year, month, day)
        )
    }

    private fun timestamp(year: Int, month: Int, day: Int): Long {
        return LocalDate.of(year, month, day)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }
}
