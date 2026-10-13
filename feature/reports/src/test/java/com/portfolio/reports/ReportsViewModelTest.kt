package com.portfolio.reports

import app.cash.turbine.test
import com.portfolio.reports.effect.ReportsEffect
import com.portfolio.reports.intent.ReportsIntent
import com.portfolio.reports.viewmodel.ReportsViewModel
import com.smartexpense.domain.model.Category
import com.smartexpense.domain.model.CategorySpending
import com.smartexpense.domain.model.DailySpending
import com.smartexpense.domain.model.ExpenseReport
import com.smartexpense.domain.usecase.CategoryUseCase
import com.smartexpense.domain.usecase.GetExpenseReportUseCase
import com.smartexpense.test.MainDispatcherRule
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

    private lateinit var viewModel: ReportsViewModel

    private val zoneId = ZoneId.systemDefault()

    @Before
    fun setUp() {
        getExpenseReportUseCase = mockk()
        categoryUseCase = mockk()

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

        viewModel = ReportsViewModel(
            getExpenseReportUseCase = getExpenseReportUseCase,
            categoryUseCase = categoryUseCase
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

        val failingViewModel = ReportsViewModel(
            getExpenseReportUseCase = getExpenseReportUseCase,
            categoryUseCase = categoryUseCase
        )

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