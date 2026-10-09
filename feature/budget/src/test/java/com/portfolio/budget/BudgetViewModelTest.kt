package com.portfolio.budget

import app.cash.turbine.test
import com.portfolio.budget.effect.BudgetEffect
import com.portfolio.budget.intent.BudgetIntent
import com.portfolio.budget.viewmodel.BudgetViewModel
import com.smartexpense.domain.model.Budget
import com.smartexpense.domain.model.Category
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.usecase.BudgetUseCase
import com.smartexpense.domain.usecase.CategoryUseCase
import com.smartexpense.domain.usecase.GetExpenseUseCase
import com.smartexpense.test.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class BudgetViewModelTest {

    @get:Rule
    val mainDispatcherRule: MainDispatcherRule = MainDispatcherRule()

    private lateinit var budgetUseCase: BudgetUseCase
    private lateinit var categoryUseCase: CategoryUseCase

    private lateinit var getExpenseUseCase: GetExpenseUseCase

    private lateinit var viewModel: BudgetViewModel

    @Before
    fun setup() {

        budgetUseCase = mockk()
        categoryUseCase = mockk()
        getExpenseUseCase = mockk()

        /*
         * BudgetViewModel calls these flows
         * when it is created.
         *
         * They must be mocked BEFORE creating
         * the ViewModel.
         */
        every {
            budgetUseCase.getBudgets()
        } returns flowOf(emptyList())

        every {
            categoryUseCase.getCategories()
        } returns flowOf(emptyList())

        every {
            getExpenseUseCase()
        } returns flowOf(emptyList())

        viewModel = BudgetViewModel(
            budgetUseCase = budgetUseCase,
            categoryUseCase = categoryUseCase,
            getExpenseUseCase = getExpenseUseCase
        )
    }

    // --------------------------------------------------
    // Form state
    // --------------------------------------------------

    @Test
    fun categoryChangedUpdatesState() = runTest {

        viewModel.onIntent(
            BudgetIntent.CategoryChanged(1L)
        )

        viewModel.uiState.test {

            awaitItem()

            val state = awaitItem()

            assertEquals(
                1L,
                state.form.categoryId
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun amountChangedUpdatesState() = runTest {

        viewModel.onIntent(
            BudgetIntent.AmountChanged("5000")
        )

        viewModel.uiState.test {

            awaitItem()

            val state = awaitItem()

            assertEquals(
                "5000",
                state.form.amount
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun startDateChangedUpdatesState() = runTest {

        val date = 1_759_276_800_000L

        viewModel.onIntent(
            BudgetIntent.StartDateChanged(date)
        )

        viewModel.uiState.test {

            awaitItem()

            val state = awaitItem()

            assertEquals(
                date,
                state.form.startDate
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun endDateChangedUpdatesState() = runTest {

        val date = 1_762_041_600_000L

        viewModel.onIntent(
            BudgetIntent.EndDateChanged(date)
        )

        viewModel.uiState.test {

            awaitItem()

            val state = awaitItem()

            assertEquals(
                date,
                state.form.endDate
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    // --------------------------------------------------
    // Add budget
    // --------------------------------------------------

    @Test
    fun addBudgetWithValidDataCallsUseCase() = runTest {

        val startDate = 1_759_276_800_000L
        val endDate = 1_762_041_600_000L

        coEvery {
            budgetUseCase.addBudget(any())
        } just Runs

        viewModel.onIntent(
            BudgetIntent.CategoryChanged(1L)
        )

        viewModel.onIntent(
            BudgetIntent.AmountChanged("5000")
        )

        viewModel.onIntent(
            BudgetIntent.StartDateChanged(startDate)
        )

        viewModel.onIntent(
            BudgetIntent.EndDateChanged(endDate)
        )

        viewModel.onIntent(
            BudgetIntent.AddBudget
        )

        advanceUntilIdle()

        coVerify(exactly = 1) {
            budgetUseCase.addBudget(
                match {
                    it.categoryId == 1L &&
                            it.amount == 5000.0 &&
                            it.startDate == startDate &&
                            it.endDate == endDate
                }
            )
        }
    }

    @Test
    fun addBudgetWithInvalidAmountShowsError() = runTest {

        viewModel.onIntent(
            BudgetIntent.CategoryChanged(1L)
        )

        viewModel.onIntent(
            BudgetIntent.AmountChanged("0")
        )

        viewModel.onIntent(
            BudgetIntent.StartDateChanged(1L)
        )

        viewModel.onIntent(
            BudgetIntent.EndDateChanged(2L)
        )

        viewModel.effect.test {

            viewModel.onIntent(
                BudgetIntent.AddBudget
            )

            assertEquals(
                BudgetEffect.ShowError(
                    "Please enter a valid budget amount"
                ),
                awaitItem()
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addBudgetWithNonNumericAmountShowsError() = runTest {

        viewModel.onIntent(
            BudgetIntent.CategoryChanged(1L)
        )

        viewModel.onIntent(
            BudgetIntent.AmountChanged("abc")
        )

        viewModel.onIntent(
            BudgetIntent.StartDateChanged(1L)
        )

        viewModel.onIntent(
            BudgetIntent.EndDateChanged(2L)
        )

        viewModel.effect.test {

            viewModel.onIntent(
                BudgetIntent.AddBudget
            )

            assertEquals(
                BudgetEffect.ShowError(
                    "Please enter a valid budget amount"
                ),
                awaitItem()
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addBudgetWithoutCategoryShowsError() = runTest {

        viewModel.onIntent(
            BudgetIntent.AmountChanged("5000")
        )

        viewModel.onIntent(
            BudgetIntent.StartDateChanged(1L)
        )

        viewModel.onIntent(
            BudgetIntent.EndDateChanged(2L)
        )

        viewModel.effect.test {

            viewModel.onIntent(
                BudgetIntent.AddBudget
            )

            assertEquals(
                BudgetEffect.ShowError(
                    "Please select a category"
                ),
                awaitItem()
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addBudgetWithoutStartDateShowsError() = runTest {

        viewModel.onIntent(
            BudgetIntent.CategoryChanged(1L)
        )

        viewModel.onIntent(
            BudgetIntent.AmountChanged("5000")
        )

        viewModel.onIntent(
            BudgetIntent.EndDateChanged(2L)
        )

        viewModel.effect.test {

            viewModel.onIntent(
                BudgetIntent.AddBudget
            )

            assertEquals(
                BudgetEffect.ShowError(
                    "Please select a start date"
                ),
                awaitItem()
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addBudgetWithoutEndDateShowsError() = runTest {

        viewModel.onIntent(
            BudgetIntent.CategoryChanged(1L)
        )

        viewModel.onIntent(
            BudgetIntent.AmountChanged("5000")
        )

        viewModel.onIntent(
            BudgetIntent.StartDateChanged(1L)
        )

        viewModel.effect.test {

            viewModel.onIntent(
                BudgetIntent.AddBudget
            )

            assertEquals(
                BudgetEffect.ShowError(
                    "Please select an end date"
                ),
                awaitItem()
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addBudgetWithInvalidDateRangeShowsError() = runTest {

        viewModel.onIntent(
            BudgetIntent.CategoryChanged(1L)
        )

        viewModel.onIntent(
            BudgetIntent.AmountChanged("5000")
        )

        viewModel.onIntent(
            BudgetIntent.StartDateChanged(10L)
        )

        viewModel.onIntent(
            BudgetIntent.EndDateChanged(5L)
        )

        viewModel.effect.test {

            viewModel.onIntent(
                BudgetIntent.AddBudget
            )

            assertEquals(
                BudgetEffect.ShowError(
                    "End date cannot be before start date"
                ),
                awaitItem()
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addBudgetEmitsBudgetAdded() = runTest {

        coEvery {
            budgetUseCase.addBudget(any())
        } just Runs

        viewModel.onIntent(
            BudgetIntent.CategoryChanged(1L)
        )

        viewModel.onIntent(
            BudgetIntent.AmountChanged("5000")
        )

        viewModel.onIntent(
            BudgetIntent.StartDateChanged(1L)
        )

        viewModel.onIntent(
            BudgetIntent.EndDateChanged(10L)
        )

        viewModel.effect.test {

            viewModel.onIntent(
                BudgetIntent.AddBudget
            )

            assertEquals(
                BudgetEffect.BudgetAdded,
                awaitItem()
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    // --------------------------------------------------
    // Load budget
    // --------------------------------------------------

    @Test
    fun loadBudgetPopulatesForm() = runTest {

        val budget = Budget(
            id = 1L,
            categoryId = 2L,
            amount = 5000.0,
            startDate = 10L,
            endDate = 20L
        )

        coEvery {
            budgetUseCase.getBudgetById(1L)
        } returns budget

        viewModel.onIntent(
            BudgetIntent.LoadBudget(1L)
        )

        advanceUntilIdle()

        viewModel.uiState.test {

            awaitItem()

            val state = awaitItem()

            assertEquals(
                2L,
                state.form.categoryId
            )

            assertEquals(
                "5000.0",
                state.form.amount
            )

            assertEquals(
                10L,
                state.form.startDate
            )

            assertEquals(
                20L,
                state.form.endDate
            )

            assertEquals(
                1L,
                state.form.editingBudgetId
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loadBudgetNotFoundShowsError() = runTest {

        coEvery {
            budgetUseCase.getBudgetById(1L)
        } returns null

        viewModel.effect.test {

            viewModel.onIntent(
                BudgetIntent.LoadBudget(1L)
            )

            advanceUntilIdle()

            assertEquals(
                BudgetEffect.ShowError(
                    "Budget not found"
                ),
                awaitItem()
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    // --------------------------------------------------
    // Update
    // --------------------------------------------------

    @Test
    fun updateBudgetCallsUseCase() = runTest {

        val existingBudget = Budget(
            id = 1L,
            categoryId = 1L,
            amount = 5000.0,
            startDate = 1L,
            endDate = 10L
        )

        coEvery {
            budgetUseCase.getBudgetById(1L)
        } returns existingBudget

        coEvery {
            budgetUseCase.updateBudget(any())
        } just Runs

        viewModel.onIntent(
            BudgetIntent.LoadBudget(1L)
        )

        advanceUntilIdle()

        viewModel.onIntent(
            BudgetIntent.CategoryChanged(2L)
        )

        viewModel.onIntent(
            BudgetIntent.AmountChanged("7500")
        )

        viewModel.onIntent(
            BudgetIntent.StartDateChanged(5L)
        )

        viewModel.onIntent(
            BudgetIntent.EndDateChanged(20L)
        )

        viewModel.onIntent(
            BudgetIntent.UpdateBudget
        )

        advanceUntilIdle()

        coVerify(exactly = 1) {
            budgetUseCase.updateBudget(
                match {
                    it.id == 1L &&
                            it.categoryId == 2L &&
                            it.amount == 7500.0 &&
                            it.startDate == 5L &&
                            it.endDate == 20L
                }
            )
        }
    }

    @Test
    fun updateBudgetEmitsBudgetUpdated() = runTest {

        val existingBudget = Budget(
            id = 1L,
            categoryId = 1L,
            amount = 5000.0,
            startDate = 1L,
            endDate = 10L
        )

        coEvery {
            budgetUseCase.getBudgetById(1L)
        } returns existingBudget

        coEvery {
            budgetUseCase.updateBudget(any())
        } just Runs

        viewModel.onIntent(
            BudgetIntent.LoadBudget(1L)
        )

        advanceUntilIdle()

        viewModel.effect.test {

            viewModel.onIntent(
                BudgetIntent.UpdateBudget
            )

            advanceUntilIdle()

            assertEquals(
                BudgetEffect.BudgetUpdated,
                awaitItem()
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    // --------------------------------------------------
    // Cancel edit
    // --------------------------------------------------

    @Test
    fun cancelEditClearsForm() = runTest {

        viewModel.onIntent(
            BudgetIntent.CategoryChanged(1L)
        )

        viewModel.onIntent(
            BudgetIntent.AmountChanged("5000")
        )

        viewModel.onIntent(
            BudgetIntent.StartDateChanged(1L)
        )

        viewModel.onIntent(
            BudgetIntent.EndDateChanged(10L)
        )

        viewModel.onIntent(
            BudgetIntent.CancelEdit
        )

        viewModel.uiState.test {

            awaitItem()

            val state = awaitItem()

            assertNull(
                state.form.categoryId
            )

            assertEquals(
                "",
                state.form.amount
            )

            assertNull(
                state.form.startDate
            )

            assertNull(
                state.form.endDate
            )

            assertNull(
                state.form.editingBudgetId
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    // --------------------------------------------------
    // Delete
    // --------------------------------------------------

    @Test
    fun requestDeleteBudgetShowsConfirmation() = runTest {

        viewModel.onIntent(
            BudgetIntent.RequestDeleteBudget(1L)
        )

        assertTrue(
            viewModel.screenState.value
                .showDeleteConfirmation
        )

        assertEquals(
            1L,
            viewModel.screenState.value
                .budgetToDeleteId
        )
    }

    @Test
    fun cancelDeleteBudgetClearsConfirmation() = runTest {

        viewModel.onIntent(
            BudgetIntent.RequestDeleteBudget(1L)
        )

        viewModel.onIntent(
            BudgetIntent.CancelDeleteBudget
        )

        assertFalse(
            viewModel.screenState.value
                .showDeleteConfirmation
        )

        assertNull(
            viewModel.screenState.value
                .budgetToDeleteId
        )
    }

    @Test
    fun confirmDeleteBudgetCallsUseCase() = runTest {

        val budget = Budget(
            id = 1L,
            categoryId = 1L,
            amount = 5000.0,
            startDate = 1L,
            endDate = 10L
        )

        coEvery {
            budgetUseCase.getBudgetById(1L)
        } returns budget

        coEvery {
            budgetUseCase.deleteBudget(any())
        } just Runs

        viewModel.onIntent(
            BudgetIntent.RequestDeleteBudget(1L)
        )

        viewModel.onIntent(
            BudgetIntent.ConfirmDeleteBudget
        )

        advanceUntilIdle()

        coVerify(exactly = 1) {
            budgetUseCase.deleteBudget(
                budget
            )
        }
    }

    @Test
    fun confirmDeleteBudgetEmitsBudgetDeleted() = runTest {

        val budget = Budget(
            id = 1L,
            categoryId = 1L,
            amount = 5000.0,
            startDate = 1L,
            endDate = 10L
        )

        coEvery {
            budgetUseCase.getBudgetById(1L)
        } returns budget

        coEvery {
            budgetUseCase.deleteBudget(any())
        } just Runs

        viewModel.onIntent(
            BudgetIntent.RequestDeleteBudget(1L)
        )

        viewModel.effect.test {

            viewModel.onIntent(
                BudgetIntent.ConfirmDeleteBudget
            )

            advanceUntilIdle()

            assertEquals(
                BudgetEffect.BudgetDeleted,
                awaitItem()
            )

            cancelAndIgnoreRemainingEvents()
        }
    }


    @Test
    fun budgetSummaryCalculatesSpentAmount() = runTest {
        val budget = Budget(
            id = 1L,
            categoryId = 1L,
            amount = 1000.0,
            startDate = dateMillis(2026, 10, 1),
            endDate = dateMillis(2026, 10, 31)
        )

        val expenses = listOf(
            Expense(
                id = 1L,
                amount = 250.0,
                categoryId = 1L,
                description = "Lunch",
                date = dateMillis(2026, 10, 5)
            ),
            Expense(
                id = 2L,
                amount = 150.0,
                categoryId = 1L,
                description = "Dinner",
                date = dateMillis(2026, 10, 10)
            )
        )

        every {
            budgetUseCase.getBudgets()
        } returns flowOf(listOf(budget))

        every {
            categoryUseCase.getCategories()
        } returns flowOf(
            listOf(
                Category(
                    id = 1L,
                    name = "Food"
                )
            )
        )

        every {
            getExpenseUseCase()
        } returns flowOf(expenses)

        viewModel = BudgetViewModel(
            budgetUseCase = budgetUseCase,
            categoryUseCase = categoryUseCase,
            getExpenseUseCase = getExpenseUseCase
        )

        viewModel.uiState.test {
            awaitItem()

            val state = awaitItem()

            val summary = state.budgetSummaries.first()

            assertEquals(400.0, summary.spentAmount, 0.001)
            assertEquals(600.0, summary.remainingAmount, 0.001)
            assertEquals(0.4f, summary.progress, 0.001f)
            assertFalse(summary.isOverBudget)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun budgetSummaryDetectsOverBudget() = runTest {
        val budget = Budget(
            id = 1L,
            categoryId = 1L,
            amount = 1000.0,
            startDate = dateMillis(2026, 10, 1),
            endDate = dateMillis(2026, 10, 31)
        )

        val expenses = listOf(
            Expense(
                id = 1L,
                amount = 700.0,
                categoryId = 1L,
                description = "Expense 1",
                date = dateMillis(2026, 10, 5)
            ),
            Expense(
                id = 2L,
                amount = 500.0,
                categoryId = 1L,
                description = "Expense 2",
                date = dateMillis(2026, 10, 10)
            )
        )

        every {
            budgetUseCase.getBudgets()
        } returns flowOf(listOf(budget))

        every {
            categoryUseCase.getCategories()
        } returns flowOf(
            listOf(
                Category(
                    id = 1L,
                    name = "Food"
                )
            )
        )

        every {
            getExpenseUseCase()
        } returns flowOf(expenses)

        viewModel = BudgetViewModel(
            budgetUseCase = budgetUseCase,
            categoryUseCase = categoryUseCase,
            getExpenseUseCase = getExpenseUseCase
        )

        viewModel.uiState.test {
            awaitItem()

            val state = awaitItem()

            val summary = state.budgetSummaries.first()

            assertEquals(1200.0, summary.spentAmount, 0.001)
            assertEquals(-200.0, summary.remainingAmount, 0.001)
            assertEquals(1f, summary.progress, 0.001f)
            assertTrue(summary.isOverBudget)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun budgetSummaryIgnoresExpenseFromDifferentCategory() = runTest {
        val budget = Budget(
            id = 1L,
            categoryId = 1L,
            amount = 1_000.0,
            startDate = dateMillis(2026, 10, 1),
            endDate = dateMillis(2026, 10, 31)
        )

        val categories = listOf(
            Category(1L, "Food"),
            Category(2L, "Transport")
        )

        val expenses = listOf(
            Expense(
                id = 1L,
                amount = 300.0,
                categoryId = 1L,
                description = "Lunch",
                date = dateMillis(2026, 10, 10)
            ),
            Expense(
                id = 2L,
                amount = 500.0,
                categoryId = 2L,
                description = "Taxi",
                date = dateMillis(2026, 10, 10)
            )
        )

        every { budgetUseCase.getBudgets() } returns flowOf(listOf(budget))
        every { categoryUseCase.getCategories() } returns flowOf(categories)
        every { getExpenseUseCase() } returns flowOf(expenses)

        viewModel = BudgetViewModel(
            budgetUseCase = budgetUseCase,
            categoryUseCase = categoryUseCase,
            getExpenseUseCase = getExpenseUseCase
        )

        viewModel.uiState.test {
            awaitItem()

            val state = awaitItem()

            assertEquals(300.0, state.budgetSummaries.first().spentAmount)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun budgetSummaryIgnoresExpenseOutsideDateRange() = runTest {
        val budget = Budget(
            id = 1L,
            categoryId = 1L,
            amount = 1_000.0,
            startDate = dateMillis(2026, 10, 10),
            endDate = dateMillis(2026, 10, 20)
        )

        val categories = listOf(
            Category(1L, "Food")
        )

        val expenses = listOf(
            Expense(
                id = 1L,
                amount = 300.0,
                categoryId = 1L,
                description = "Inside range",
                date = dateMillis(2026, 10, 15)
            ),
            Expense(
                id = 2L,
                amount = 500.0,
                categoryId = 1L,
                description = "Outside range",
                date = dateMillis(2026, 10, 25)
            )
        )

        every { budgetUseCase.getBudgets() } returns flowOf(listOf(budget))
        every { categoryUseCase.getCategories() } returns flowOf(categories)
        every { getExpenseUseCase() } returns flowOf(expenses)

        viewModel = BudgetViewModel(
            budgetUseCase = budgetUseCase,
            categoryUseCase = categoryUseCase,
            getExpenseUseCase = getExpenseUseCase
        )

        viewModel.uiState.test {
            awaitItem()

            val state = awaitItem()

            assertEquals(300.0, state.budgetSummaries.first().spentAmount)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun budgetSummaryIncludesExpenseOnEndDate() = runTest {
        val endDate = dateMillis(2026, 10, 20)

        val budget = Budget(
            id = 1L,
            categoryId = 1L,
            amount = 1_000.0,
            startDate = dateMillis(2026, 10, 10),
            endDate = endDate
        )

        val expenses = listOf(
            Expense(
                id = 1L,
                amount = 350.0,
                categoryId = 1L,
                description = "End date expense",
                date = endDate
            )
        )

        every { budgetUseCase.getBudgets() } returns flowOf(listOf(budget))
        every { categoryUseCase.getCategories() } returns flowOf(
            listOf(Category(1L, "Food"))
        )
        every { getExpenseUseCase() } returns flowOf(expenses)

        viewModel = BudgetViewModel(
            budgetUseCase = budgetUseCase,
            categoryUseCase = categoryUseCase,
            getExpenseUseCase = getExpenseUseCase
        )

        viewModel.uiState.test {
            awaitItem()

            val state = awaitItem()

            assertEquals(350.0, state.budgetSummaries.first().spentAmount)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun budgetSummaryReturnsZeroSpentWhenThereAreNoExpenses() = runTest {
        val budget = Budget(
            id = 1L,
            categoryId = 1L,
            amount = 1_000.0,
            startDate = dateMillis(2026, 10, 1),
            endDate = dateMillis(2026, 10, 31)
        )

        every { budgetUseCase.getBudgets() } returns flowOf(listOf(budget))
        every { categoryUseCase.getCategories() } returns flowOf(
            listOf(Category(1L, "Food"))
        )
        every { getExpenseUseCase() } returns flowOf(emptyList())

        viewModel = BudgetViewModel(
            budgetUseCase = budgetUseCase,
            categoryUseCase = categoryUseCase,
            getExpenseUseCase = getExpenseUseCase
        )

        viewModel.uiState.test {
            awaitItem()

            val state = awaitItem()
            val summary = state.budgetSummaries.first()

            assertEquals(0.0, summary.spentAmount)
            assertEquals(1_000.0, summary.remainingAmount)
            assertEquals(0f, summary.progress)
            assertEquals(false, summary.isOverBudget)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun multipleBudgetsCalculateIndependently() = runTest {
        val foodBudget = Budget(
            id = 1L,
            categoryId = 1L,
            amount = 1_000.0,
            startDate = dateMillis(2026, 10, 1),
            endDate = dateMillis(2026, 10, 31)
        )

        val transportBudget = Budget(
            id = 2L,
            categoryId = 2L,
            amount = 2_000.0,
            startDate = dateMillis(2026, 10, 1),
            endDate = dateMillis(2026, 10, 31)
        )

        val categories = listOf(
            Category(1L, "Food"),
            Category(2L, "Transport")
        )

        val expenses = listOf(
            Expense(
                id = 1L,
                amount = 300.0,
                categoryId = 1L,
                description = "Lunch",
                date = dateMillis(2026, 10, 10)
            ),
            Expense(
                id = 2L,
                amount = 500.0,
                categoryId = 2L,
                description = "Taxi",
                date = dateMillis(2026, 10, 10)
            )
        )

        every { budgetUseCase.getBudgets() } returns flowOf(
            listOf(foodBudget, transportBudget)
        )
        every { categoryUseCase.getCategories() } returns flowOf(categories)
        every { getExpenseUseCase() } returns flowOf(expenses)

        viewModel = BudgetViewModel(
            budgetUseCase = budgetUseCase,
            categoryUseCase = categoryUseCase,
            getExpenseUseCase = getExpenseUseCase
        )

        viewModel.uiState.test {
            awaitItem()

            val state = awaitItem()

            val foodSummary = state.budgetSummaries
                .first { it.budget.id == 1L }

            val transportSummary = state.budgetSummaries
                .first { it.budget.id == 2L }

            assertEquals(300.0, foodSummary.spentAmount)
            assertEquals(700.0, foodSummary.remainingAmount)

            assertEquals(500.0, transportSummary.spentAmount)
            assertEquals(1_500.0, transportSummary.remainingAmount)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun budgetSummaryUpdatesWhenExpensesChange() = runTest {
        val budget = Budget(
            id = 1L,
            categoryId = 1L,
            amount = 1_000.0,
            startDate = dateMillis(2026, 10, 1),
            endDate = dateMillis(2026, 10, 31)
        )

        val expensesFlow = MutableStateFlow(
            listOf(
                Expense(
                    id = 1L,
                    amount = 300.0,
                    categoryId = 1L,
                    description = "Lunch",
                    date = dateMillis(2026, 10, 10)
                )
            )
        )

        every { budgetUseCase.getBudgets() } returns flowOf(listOf(budget))
        every { categoryUseCase.getCategories() } returns flowOf(
            listOf(Category(1L, "Food"))
        )
        every { getExpenseUseCase() } returns expensesFlow

        viewModel = BudgetViewModel(
            budgetUseCase = budgetUseCase,
            categoryUseCase = categoryUseCase,
            getExpenseUseCase = getExpenseUseCase
        )

        viewModel.uiState.test {
            awaitItem()

            var state = awaitItem()
            assertEquals(300.0, state.budgetSummaries.first().spentAmount)

            expensesFlow.value += Expense(
                            id = 2L,
                            amount = 200.0,
                            categoryId = 1L,
                            description = "Dinner",
                            date = dateMillis(2026, 10, 11)
                        )

            state = awaitItem()

            assertEquals(500.0, state.budgetSummaries.first().spentAmount)
            assertEquals(500.0, state.budgetSummaries.first().remainingAmount)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun dateMillis(
        year: Int,
        month: Int,
        day: Int
    ): Long {
        return LocalDate.of(year, month, day)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}