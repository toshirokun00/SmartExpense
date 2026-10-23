package com.smartexpense.budget

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartexpense.budget.screen.BudgetScreen
import com.smartexpense.budget.viewmodel.BudgetViewModel
import com.smartexpense.domain.model.AppSettings
import com.smartexpense.domain.model.Budget
import com.smartexpense.domain.model.Category
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.usecase.BudgetUseCase
import com.smartexpense.domain.usecase.CategoryUseCase
import com.smartexpense.domain.usecase.GetBudgetInsightsUseCase
import com.smartexpense.domain.usecase.GetExpenseUseCase
import com.smartexpense.domain.usecase.SettingsUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class BudgetScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var budgetUseCase: BudgetUseCase
    private lateinit var categoryUseCase: CategoryUseCase
    private lateinit var getExpenseUseCase: GetExpenseUseCase
    private lateinit var settingsUseCase: SettingsUseCase

    @Before
    fun setup() {
        budgetUseCase = mockk()
        categoryUseCase = mockk()
        getExpenseUseCase = mockk()
        settingsUseCase = mockk()

        every { categoryUseCase.getCategories() } returns flowOf(
            listOf(Category(1L, "Food"))
        )
        every { settingsUseCase.getSettings() } returns flowOf(
            AppSettings(budgetAlertThresholds = setOf(80, 100))
        )
    }

    private fun dateMillis(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private fun buildViewModel(spent: Double): BudgetViewModel {
        val budget = Budget(
            id = 1L,
            categoryId = 1L,
            amount = 1_000.0,
            startDate = dateMillis(2026, 10, 1),
            endDate = dateMillis(2026, 10, 31)
        )

        every { budgetUseCase.getBudgets() } returns flowOf(listOf(budget))
        every { getExpenseUseCase() } returns flowOf(
            listOf(
                Expense(
                    id = 1L,
                    amount = spent,
                    categoryId = 1L,
                    description = "Groceries",
                    date = dateMillis(2026, 10, 10)
                )
            )
        )

        return BudgetViewModel(
            budgetUseCase = budgetUseCase,
            categoryUseCase = categoryUseCase,
            getExpenseUseCase = getExpenseUseCase,
            settingsUseCase = settingsUseCase,
            getBudgetInsightsUseCase = GetBudgetInsightsUseCase()
        )
    }

    private fun awaitText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithText(text, substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun scrollTo(text: String) {
        composeRule
            .onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText(text, substring = true))
    }

    @Test
    fun showsValidationErrorInSnackbar() {
        val viewModel = buildViewModel(spent = 0.0)

        composeRule.setContent {
            BudgetScreen(viewModel = viewModel, onNavigateBack = {})
        }

        awaitText("Budget Amount")

        composeRule
            .onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText("Add Budget") and hasClickAction())

        composeRule
            .onNode(hasText("Add Budget") and hasClickAction())
            .performClick()

        awaitText("Please enter a valid budget amount")

        composeRule
            .onNodeWithText("Please enter a valid budget amount")
            .assertIsDisplayed()
    }

    @Test
    fun duplicateCategoryBudgetsRenderInsightsWithoutKeyCollision() {
        val first = Budget(
            id = 1L,
            categoryId = 1L,
            amount = 1_000.0,
            startDate = dateMillis(2026, 10, 1),
            endDate = dateMillis(2026, 10, 31)
        )
        val second = first.copy(id = 2L, amount = 900.0)

        every { budgetUseCase.getBudgets() } returns flowOf(listOf(first, second))
        every { getExpenseUseCase() } returns flowOf(
            listOf(
                Expense(
                    id = 1L,
                    amount = 850.0,
                    categoryId = 1L,
                    description = "Groceries",
                    date = dateMillis(2026, 10, 10)
                )
            )
        )

        val viewModel = BudgetViewModel(
            budgetUseCase = budgetUseCase,
            categoryUseCase = categoryUseCase,
            getExpenseUseCase = getExpenseUseCase,
            settingsUseCase = settingsUseCase,
            getBudgetInsightsUseCase = GetBudgetInsightsUseCase()
        )

        composeRule.setContent {
            BudgetScreen(viewModel = viewModel, onNavigateBack = {})
        }

        composeRule
            .onNode(hasScrollToNodeAction())
            .performScrollToNode(
                hasText(
                    "You've used 94% of your Food budget",
                    substring = true
                )
            )

        composeRule
            .onNodeWithText(
                "You've used 94% of your Food budget",
                substring = true
            )
            .assertIsDisplayed()
    }

    @Test
    fun showsUtilizationAndApproachingStatus() {
        val viewModel = buildViewModel(spent = 850.0)

        composeRule.setContent {
            BudgetScreen(viewModel = viewModel, onNavigateBack = {})
        }

        awaitText("85% used")

        composeRule.onNodeWithText("Budgets").assertIsDisplayed()
        composeRule
            .onNodeWithText("85% used • Approaching limit")
            .assertIsDisplayed()

        awaitText("Insights")
        scrollTo("You've used 85% of your Food budget")
        composeRule
            .onNodeWithText(
                "You've used 85% of your Food budget",
                substring = true
            )
            .assertIsDisplayed()
    }

    @Test
    fun showsExceededIndicatorAndInsight() {
        val viewModel = buildViewModel(spent = 1_200.0)

        composeRule.setContent {
            BudgetScreen(viewModel = viewModel, onNavigateBack = {})
        }

        awaitText("120% used")

        composeRule
            .onNodeWithText("120% used • Exceeded limit")
            .assertIsDisplayed()

        awaitText("Insights")
        scrollTo("You're over your Food budget by")
        composeRule
            .onNodeWithText(
                "You're over your Food budget by",
                substring = true
            )
            .assertIsDisplayed()
    }
}
