package com.smartexpense.reports

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartexpense.domain.model.Category
import com.smartexpense.domain.model.CategorySpending
import com.smartexpense.domain.model.DailySpending
import com.smartexpense.domain.model.ExpenseReport
import com.smartexpense.domain.usecase.CategoryUseCase
import com.smartexpense.domain.usecase.GetExpenseReportUseCase
import com.smartexpense.reports.screen.ReportsScreen
import com.smartexpense.reports.viewmodel.ReportsViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for [ReportsScreen].
 *
 * Uses a real ViewModel backed by mock use-cases so the composable renders
 * actual state without needing Koin or a real database.
 */
@RunWith(AndroidJUnit4::class)
class ReportsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var getExpenseReportUseCase: GetExpenseReportUseCase
    private lateinit var categoryUseCase: CategoryUseCase
    private lateinit var viewModel: ReportsViewModel

    @Before
    fun setUp() {
        getExpenseReportUseCase = mockk()
        categoryUseCase = mockk()

        every { categoryUseCase.getCategories() } returns flowOf(
            listOf(
                Category(id = 1L, name = "Food"),
                Category(id = 2L, name = "Transport")
            )
        )

        every { getExpenseReportUseCase(any(), any()) } returns flowOf(
            ExpenseReport(
                totalAmount = 500.0,
                expenseCount = 4,
                averageAmount = 125.0,
                categoryBreakdown = listOf(
                    CategorySpending(categoryId = 1L, totalAmount = 300.0, percentage = 60.0),
                    CategorySpending(categoryId = 2L, totalAmount = 200.0, percentage = 40.0)
                ),
                dailySpending = listOf(
                    DailySpending(date = 1_760_000_000_000L, totalAmount = 250.0),
                    DailySpending(date = 1_760_100_000_000L, totalAmount = 250.0)
                )
            )
        )

        viewModel = ReportsViewModel(
            getExpenseReportUseCase = getExpenseReportUseCase,
            categoryUseCase = categoryUseCase
        )
    }

    // --- initial render ---

    @Test
    fun reportsTitleIsDisplayed() {
        setContent()

        composeRule.onNodeWithText("Expense Reports").assertIsDisplayed()
    }

    @Test
    fun startDateAndEndDateButtonsAreDisplayed() {
        setContent()

        composeRule.onNodeWithText("Start Date").assertIsDisplayed()
        composeRule.onNodeWithText("End Date").assertIsDisplayed()
    }

    // --- summary card ---

    @Test
    fun totalSpendingLabelIsDisplayed() {
        setContent()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Total Spending").assertIsDisplayed()
    }

    @Test
    fun totalAmountIsDisplayed() {
        setContent()
        composeRule.waitForIdle()

        // ₱500.00 formatted with Philippine locale.
        composeRule.onNodeWithText("₱500.00").assertIsDisplayed()
    }

    @Test
    fun expenseCountIsDisplayed() {
        setContent()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("4").assertIsDisplayed()
    }

    @Test
    fun averageAmountIsDisplayed() {
        setContent()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("₱125.00").assertIsDisplayed()
    }

    // --- category breakdown ---

    @Test
    fun spendingByCategorySectionTitleIsDisplayed() {
        setContent()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Spending by Category").assertIsDisplayed()
    }

    @Test
    fun categoryNamesAreDisplayed() {
        setContent()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Food").assertIsDisplayed()
        composeRule.onNodeWithText("Transport").assertIsDisplayed()
    }

    @Test
    fun categoryAmountsAreDisplayed() {
        setContent()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("₱300.00").assertIsDisplayed()
        composeRule.onNodeWithText("₱200.00").assertIsDisplayed()
    }

    @Test
    fun categoryPercentagesAreDisplayed() {
        setContent()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("60.0%").assertIsDisplayed()
        composeRule.onNodeWithText("40.0%").assertIsDisplayed()
    }

    // --- daily spending ---

    @Test
    fun dailySpendingSectionTitleIsDisplayed() {
        setContent()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Daily Spending").assertIsDisplayed()
    }

    // --- empty state ---

    @Test
    fun emptyCategoryMessageIsShownWhenNoCategoryData() {
        every { getExpenseReportUseCase(any(), any()) } returns flowOf(
            ExpenseReport(categoryBreakdown = emptyList(), dailySpending = emptyList())
        )

        viewModel = ReportsViewModel(
            getExpenseReportUseCase = getExpenseReportUseCase,
            categoryUseCase = categoryUseCase
        )

        setContent()
        composeRule.waitForIdle()

        composeRule
            .onNodeWithText("No category spending for this period.")
            .assertIsDisplayed()
    }

    @Test
    fun emptyDailySpendingMessageIsShownWhenNoDailyData() {
        every { getExpenseReportUseCase(any(), any()) } returns flowOf(
            ExpenseReport(categoryBreakdown = emptyList(), dailySpending = emptyList())
        )

        viewModel = ReportsViewModel(
            getExpenseReportUseCase = getExpenseReportUseCase,
            categoryUseCase = categoryUseCase
        )

        setContent()
        composeRule.waitForIdle()

        composeRule
            .onNodeWithText("No daily spending for this period.")
            .assertIsDisplayed()
    }

    // --- error state ---

    @Test
    fun errorMessageIsDisplayedWhenReportLoadFails() {
        every {
            getExpenseReportUseCase(any(), any())
        } returns flow {
            throw IllegalStateException("Failed to load report")
        }

        viewModel = ReportsViewModel(
            getExpenseReportUseCase = getExpenseReportUseCase,
            categoryUseCase = categoryUseCase
        )

        setContent()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Failed to load report").assertIsDisplayed()
    }

    @Test
    fun retryButtonIsDisplayedOnError() {
        every {
            getExpenseReportUseCase(any(), any())
        } returns flow {
            throw IllegalStateException("Network error")
        }

        viewModel = ReportsViewModel(
            getExpenseReportUseCase = getExpenseReportUseCase,
            categoryUseCase = categoryUseCase
        )

        setContent()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun tappingRetryReloadsTheReport() {
        var loadCount = 0

        every {
            getExpenseReportUseCase(any(), any())
        } answers {
            loadCount++
            flow { throw IllegalStateException("Network error") }
        }

        viewModel = ReportsViewModel(
            getExpenseReportUseCase = getExpenseReportUseCase,
            categoryUseCase = categoryUseCase
        )

        setContent()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Retry").performClick()
        composeRule.waitForIdle()

        // First load on init + one retry = 2 calls.
        assert(loadCount >= 2) {
            "Expected at least 2 report load calls after retry, got $loadCount"
        }
    }

    // --- date picker ---

    @Test
    fun tappingStartDateOpensDatePicker() {
        setContent()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Start Date").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Select start date").assertIsDisplayed()
    }

    @Test
    fun tappingEndDateOpensDatePicker() {
        setContent()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("End Date").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Select end date").assertIsDisplayed()
    }

    @Test
    fun dismissingDatePickerClosesIt() {
        setContent()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Start Date").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Select start date").assertDoesNotExist()
    }

    // --- helpers ---

    private fun setContent() {
        composeRule.setContent {
            ReportsScreen(viewModel = viewModel)
        }
    }
}
