package com.smartexpense.feature.expenses

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartexpense.domain.model.Category
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.model.SearchCriteria
import com.smartexpense.domain.model.SortOrder
import com.smartexpense.domain.usecase.CategoryUseCase
import com.smartexpense.domain.usecase.ScheduleSyncUseCase
import com.smartexpense.domain.usecase.SearchExpensesUseCase
import com.smartexpense.feature.expenses.screen.TransactionScreen
import com.smartexpense.feature.expenses.viewmodel.TransactionViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var searchExpensesUseCase: SearchExpensesUseCase
    private lateinit var categoryUseCase: CategoryUseCase
    private lateinit var scheduleSyncUseCase: ScheduleSyncUseCase
    private lateinit var viewModel: TransactionViewModel

    @Before
    fun setup() {
        searchExpensesUseCase = mockk(relaxed = true)
        categoryUseCase = mockk(relaxed = true)
        scheduleSyncUseCase = mockk(relaxed = true)

        every { categoryUseCase.getCategories() } returns flowOf(
            listOf(
                Category(id = 1L, name = "Food"),
                Category(id = 2L, name = "Transport")
            )
        )
        coEvery { searchExpensesUseCase(any()) } returns flowOf(emptyList())

        viewModel = TransactionViewModel(
            searchExpensesUseCase = searchExpensesUseCase,
            categoryUseCase = categoryUseCase,
            scheduleSyncUseCase = scheduleSyncUseCase
        )
    }

    @Test
    fun screenDisplaysTitle() {
        composeRule.setContent {
            TransactionScreen(
                viewModel = viewModel,
                onExpenseClick = {},
                onNavigateBack = {}
            )
        }

        composeRule.onNodeWithText("Transactions").assertIsDisplayed()
    }

    @Test
    fun searchFieldIsDisplayed() {
        composeRule.setContent {
            TransactionScreen(
                viewModel = viewModel,
                onExpenseClick = {},
                onNavigateBack = {}
            )
        }

        composeRule.onNodeWithText("Search description").assertIsDisplayed()
    }

    @Test
    fun filterToggleIsDisplayed() {
        composeRule.setContent {
            TransactionScreen(
                viewModel = viewModel,
                onExpenseClick = {},
                onNavigateBack = {}
            )
        }

        composeRule.onNodeWithText("Filters").assertIsDisplayed()
    }

    @Test
    fun enteringSearchTextUpdatesResults() {
        coEvery { searchExpensesUseCase(any()) } returns flowOf(
            listOf(
                Expense(
                    id = 1L,
                    amount = 10.0,
                    categoryId = 1L,
                    description = "Coffee",
                    date = 1_760_000_000_000L
                )
            )
        )

        composeRule.setContent {
            TransactionScreen(
                viewModel = viewModel,
                onExpenseClick = {},
                onNavigateBack = {}
            )
        }

        composeRule.onNodeWithText("Search description").performTextInput("Coffee")
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Coffee").assertIsDisplayed()
    }

    @Test
    fun categoryFilterShowsAvailableCategories() {
        composeRule.setContent {
            TransactionScreen(
                viewModel = viewModel,
                onExpenseClick = {},
                onNavigateBack = {}
            )
        }

        composeRule.onNodeWithText("Filters").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Food").assertIsDisplayed()
        composeRule.onNodeWithText("Transport").assertIsDisplayed()
    }

    @Test
    fun sortOptionsAreDisplayedWhenFilterExpanded() {
        composeRule.setContent {
            TransactionScreen(
                viewModel = viewModel,
                onExpenseClick = {},
                onNavigateBack = {}
            )
        }

        composeRule.onNodeWithText("Filters").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Newest").assertIsDisplayed()
        composeRule.onNodeWithText("Oldest").assertIsDisplayed()
        composeRule.onNodeWithText("Highest Amount").assertIsDisplayed()
        composeRule.onNodeWithText("Lowest Amount").assertIsDisplayed()
    }

    @Test
    fun clearFiltersButtonIsShownWhenFiltersAreActive() {
        coEvery { searchExpensesUseCase(any()) } returns flowOf(
            listOf(
                Expense(
                    id = 1L,
                    amount = 10.0,
                    categoryId = 1L,
                    description = "Coffee",
                    date = 1_760_000_000_000L
                )
            )
        )

        composeRule.setContent {
            TransactionScreen(
                viewModel = viewModel,
                onExpenseClick = {},
                onNavigateBack = {}
            )
        }

        composeRule.onNodeWithText("Search description").performTextInput("Coffee")
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Clear All").assertIsDisplayed()
    }

    @Test
    fun emptyStateShownWhenNoResults() {
        coEvery { searchExpensesUseCase(any()) } returns flowOf(emptyList())

        composeRule.setContent {
            TransactionScreen(
                viewModel = viewModel,
                onExpenseClick = {},
                onNavigateBack = {}
            )
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("No transactions yet").assertIsDisplayed()
    }

    @Test
    fun emptyStateShownWhenNoMatches() {
        coEvery { searchExpensesUseCase(any()) } returns flowOf(emptyList())

        composeRule.setContent {
            TransactionScreen(
                viewModel = viewModel,
                onExpenseClick = {},
                onNavigateBack = {}
            )
        }

        composeRule.onNodeWithText("Search description").performTextInput("Nonexistent")
        composeRule.waitForIdle()

        composeRule.onNodeWithText("No transactions match your filters").assertIsDisplayed()
    }

    @Test
    fun expenseRowIsClickable() {
        coEvery { searchExpensesUseCase(any()) } returns flowOf(
            listOf(
                Expense(
                    id = 42L,
                    amount = 10.0,
                    categoryId = 1L,
                    description = "Coffee",
                    date = 1_760_000_000_000L
                )
            )
        )

        var clickedExpenseId: Long? = null

        composeRule.setContent {
            TransactionScreen(
                viewModel = viewModel,
                onExpenseClick = { id ->
                    clickedExpenseId = id
                },
                onNavigateBack = {}
            )
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Coffee").performClick()
        composeRule.waitForIdle()

        assert(clickedExpenseId == 42L) { "Expected expense id 42, got $clickedExpenseId" }
    }

    @Test
    fun loadingStateIsShownInitially() {
        composeRule.setContent {
            TransactionScreen(
                viewModel = viewModel,
                onExpenseClick = {},
                onNavigateBack = {}
            )
        }

        composeRule.onNodeWithText("Loading transactions...").assertIsDisplayed()
    }
}
