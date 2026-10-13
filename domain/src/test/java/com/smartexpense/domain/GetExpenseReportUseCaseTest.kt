package com.smartexpense.domain
import com.smartexpense.domain.model.CategorySpending
import com.smartexpense.domain.model.DailySpending
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.repository.ExpenseRepository
import com.smartexpense.domain.usecase.GetExpenseReportUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class GetExpenseReportUseCaseTest {

    private lateinit var repository: ExpenseRepository
    private lateinit var useCase: GetExpenseReportUseCase

    private val zoneId = ZoneId.systemDefault()

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetExpenseReportUseCase(repository)
    }

    @Test
    fun invokeCalculatesTotalCountAndAverage()= runTest {
        val start = timestamp(2026, 10, 1)
        val end = timestamp(2026, 11, 1)

        every { repository.getExpense() } returns flowOf(
            listOf(
                expense(100.0, 1L, 2026, 10, 2),
                expense(200.0, 1L, 2026, 10, 3),
                expense(150.0, 2L, 2026, 10, 4)
            )
        )

        val report = useCase(start, end).first()

        assertEquals(450.0, report.totalAmount, 0.001)
        assertEquals(3, report.expenseCount)
        assertEquals(150.0, report.averageAmount, 0.001)
    }

    @Test
    fun invokeGroupsExpensesByCategoryAndSortsByAmount() = runTest {
        val start = timestamp(2026, 10, 1)
        val end = timestamp(2026, 11, 1)

        every { repository.getExpense() } returns flowOf(
            listOf(
                expense(100.0, 1L, 2026, 10, 2),
                expense(300.0, 2L, 2026, 10, 3),
                expense(100.0, 1L, 2026, 10, 4)
            )
        )

        val report = useCase(start, end).first()

        assertEquals(2, report.categoryBreakdown.size)

        val highest = report.categoryBreakdown[0]
        assertEquals(2L, highest.categoryId)
        assertEquals(300.0, highest.totalAmount, 0.001)
        assertEquals(60.0, highest.percentage, 0.001)

        val second = report.categoryBreakdown[1]
        assertEquals(1L, second.categoryId)
        assertEquals(200.0, second.totalAmount, 0.001)
        assertEquals(40.0, second.percentage, 0.001)
    }

    @Test
    fun invokeGroupsExpensesByCalendarDay() = runTest {
        val start = timestamp(2026, 10, 1)
        val end = timestamp(2026, 11, 1)

        every { repository.getExpense() } returns flowOf(
            listOf(
                expense(100.0, 1L, 2026, 10, 2),
                expense(50.0, 2L, 2026, 10, 2),
                expense(200.0, 1L, 2026, 10, 3)
            )
        )

        val report = useCase(start, end).first()

        assertEquals(2, report.dailySpending.size)
        assertEquals(timestamp(2026, 10, 2), report.dailySpending[0].date)
        assertEquals(150.0, report.dailySpending[0].totalAmount, 0.001)
        assertEquals(timestamp(2026, 10, 3), report.dailySpending[1].date)
        assertEquals(200.0, report.dailySpending[1].totalAmount, 0.001)
    }

    @Test
    fun invokeExcludesExpensesOutsideSelectedDateRange() = runTest {
        val start = timestamp(2026, 10, 1)
        val end = timestamp(2026, 11, 1)

        every { repository.getExpense() } returns flowOf(
            listOf(
                expense(100.0, 1L, 2026, 9, 30),
                expense(200.0, 1L, 2026, 10, 1),
                expense(300.0, 1L, 2026, 10, 31),
                expense(400.0, 1L, 2026, 11, 1)
            )
        )

        val report = useCase(start, end).first()

        assertEquals(500.0, report.totalAmount, 0.001)
        assertEquals(2, report.expenseCount)
    }

    @Test
    fun invokeReturnsZeroValuesWhenThereAreNoMatchingExpenses() = runTest {
        val start = timestamp(2026, 10, 1)
        val end = timestamp(2026, 11, 1)

        every { repository.getExpense() } returns flowOf(emptyList())

        val report = useCase(start, end).first()

        assertEquals(0.0, report.totalAmount, 0.001)
        assertEquals(0, report.expenseCount)
        assertEquals(0.0, report.averageAmount, 0.001)
        assertEquals(emptyList<CategorySpending>(), report.categoryBreakdown)
        assertEquals(emptyList<DailySpending>(), report.dailySpending)
    }

    @Test
    fun invokeRejectsInvalidDateRange() {
        val start = timestamp(2026, 10, 10)
        val end = timestamp(2026, 10, 1)

        assertThrows(IllegalArgumentException::class.java) {
            useCase(start, end)
        }
    }

    private fun expense(
        amount: Double,
        categoryId: Long,
        year: Int,
        month: Int,
        day: Int
    ): Expense {
        return Expense(
            amount = amount,
            categoryId = categoryId,
            description = "Test expense",
            date = timestamp(year, month, day)
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