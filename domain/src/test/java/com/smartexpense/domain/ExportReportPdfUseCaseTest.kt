package com.smartexpense.domain

import com.smartexpense.domain.export.ReportExportContent
import com.smartexpense.domain.export.ReportExportFormat
import com.smartexpense.domain.export.ReportExportRequest
import com.smartexpense.domain.export.ReportExportType
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.repository.ExpenseRepository
import com.smartexpense.domain.usecase.ExportReportPdfUseCase
import com.smartexpense.domain.usecase.GetExpenseReportUseCase
import com.smartexpense.domain.usecase.GetExpensesInRangeUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ExportReportPdfUseCaseTest {

    private lateinit var repository: ExpenseRepository
    private lateinit var useCase: ExportReportPdfUseCase

    private val zoneId = ZoneId.of("UTC")

    @Before
    fun setUp() {
        repository = mockk()
        useCase = ExportReportPdfUseCase(
            getExpenseReportUseCase = GetExpenseReportUseCase(repository),
            getExpensesInRangeUseCase = GetExpensesInRangeUseCase(repository),
            zoneId = zoneId
        )
    }

    @Test
    fun transactionPdfUsesInclusiveRangeAndDeterministicOrdering() = runTest {
        every { repository.getExpense() } returns flowOf(
            listOf(
                expense(2L, 20.0, 2026, 1, 31),
                expense(1L, 10.0, 2026, 1, 1),
                expense(3L, 30.0, 2026, 2, 1)
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
        assertEquals(ReportExportFormat.PDF, result.format)
        assertEquals(
            "smartexpense_transactions_2026-01-01_2026-01-31.pdf",
            result.fileName
        )
        assertEquals(2, result.recordCount)
        assertFalse(result.isEmpty)

        val data = (result.content as ReportExportContent.Pdf).data

        assertEquals(ReportExportType.TRANSACTIONS, data.type)
        assertEquals(listOf(1L, 2L), data.expenses.map(Expense::id))
        assertEquals(mapOf(1L to "Food"), data.categoryNames)
        assertNull(data.report)
    }

    @Test
    fun summaryPdfCarriesExistingReportCalculations() = runTest {
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
        assertEquals(ReportExportFormat.PDF, result.format)
        assertEquals(
            "smartexpense_report_2026-01-01_2026-01-31.pdf",
            result.fileName
        )
        assertEquals(2, result.recordCount)
        assertFalse(result.isEmpty)

        val data = (result.content as ReportExportContent.Pdf).data
        val report = requireNotNull(data.report)

        assertEquals(300.0, report.totalAmount, 0.001)
        assertEquals(2, report.expenseCount)
        assertEquals(150.0, report.averageAmount, 0.001)
        assertEquals(2, report.categoryBreakdown.size)
        assertEquals(1, report.dailySpending.size)
        assertEquals(mapOf(1L to "Food", 2L to "Transport"), data.categoryNames)
        assertTrue(data.expenses.isEmpty())
    }

    @Test
    fun emptyTransactionPdfIsMarkedEmpty() = runTest {
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
            "smartexpense_transactions_2026-01-01_2026-01-31.pdf",
            result.fileName
        )
    }

    @Test
    fun invalidDateRangeIsRejectedExplicitly() = runTest {
        val exception = runCatching {
            useCase(
                request = request(
                    type = ReportExportType.SUMMARY,
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
