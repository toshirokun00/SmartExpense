package com.smartexpense.dashoard

import app.cash.turbine.test
import com.smartexpense.dashoard.viewmodel.DashboardViewModel
import com.smartexpense.domain.model.Budget
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.usecase.BudgetUseCase
import com.smartexpense.domain.usecase.GetExpenseUseCase
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DashboardViewModelTest {

    private lateinit var getExpenseUseCase: GetExpenseUseCase

    private lateinit var budgetUseCase: BudgetUseCase
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setup() {
        getExpenseUseCase = mockk()
        budgetUseCase = mockk()

        every {
            getExpenseUseCase()
        } returns flowOf(emptyList())

        every {
            budgetUseCase.getBudgets()
        } returns flowOf(emptyList())

        viewModel = DashboardViewModel(
            getExpenseUseCase = getExpenseUseCase,
            budgetUseCase = budgetUseCase
        )
    }

    @Test
    fun dashboardCalculatesTotalExpenses() = runTest {
        val expenses = listOf(
            Expense(
                id = 1L,
                amount = 100.0,
                categoryId = 1L,
                description = "Lunch",
                date = 1L
            ),
            Expense(
                id = 2L,
                amount = 250.50,
                categoryId = 1L,
                description = "Groceries",
                date = 2L
            ),
            Expense(
                id = 3L,
                amount = 75.25,
                categoryId = 2L,
                description = "Transport",
                date = 3L
            )
        )

        every {
            getExpenseUseCase()
        } returns flowOf(expenses)

        viewModel = DashboardViewModel(
            getExpenseUseCase = getExpenseUseCase,
            budgetUseCase = budgetUseCase
        )

        viewModel.uiState.test {
            awaitItem()

            val state = awaitItem()

            assertEquals(
                425.75,
                state.totalExpense,
                0.001
            )

            assertEquals(
                3,
                state.expenseCount
            )

            assertEquals(
                3,
                state.recentExpenses.size
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun totalExpenseIsCalculatedFromExpenses() = runTest {
        val expenses = listOf(
            Expense(
                id = 1L,
                amount = 100.0,
                categoryId = 1L,
                description = "Lunch",
                date = dateMillis(2026, 10, 1)
            ),
            Expense(
                id = 2L,
                amount = 250.0,
                categoryId = 1L,
                description = "Dinner",
                date = dateMillis(2026, 10, 2)
            )
        )

        every { getExpenseUseCase() } returns flowOf(expenses)

        viewModel = DashboardViewModel(
            getExpenseUseCase,
            budgetUseCase
        )

        viewModel.uiState.test {
            awaitItem()

            val state = awaitItem()

            assertEquals(350.0, state.totalExpense)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun budgetSummaryIsCalculatedCorrectly() = runTest {
        val budget = Budget(
            id = 1L,
            categoryId = 1L,
            amount = 1_000.0,
            startDate = dateMillis(2026, 10, 1),
            endDate = dateMillis(2026, 10, 31)
        )

        val expenses = listOf(
            Expense(
                id = 1L,
                amount = 300.0,
                categoryId = 1L,
                description = "Lunch",
                date = dateMillis(2026, 10, 10)
            )
        )

        every { getExpenseUseCase() } returns flowOf(expenses)
        every { budgetUseCase.getBudgets() } returns flowOf(listOf(budget))

        viewModel = DashboardViewModel(
            getExpenseUseCase,
            budgetUseCase
        )

        viewModel.uiState.test {
            awaitItem()

            val state = awaitItem()

            assertEquals(1_000.0, state.totalBudget)
            assertEquals(300.0, state.totalBudgetSpent)
            assertEquals(700.0, state.totalBudgetRemaining)
            assertEquals(0.3f, state.budgetProgress)
            assertEquals(false, state.isOverBudget)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun dashboardDetectsOverBudget() = runTest {
        val budget = Budget(
            id = 1L,
            categoryId = 1L,
            amount = 500.0,
            startDate = dateMillis(2026, 10, 1),
            endDate = dateMillis(2026, 10, 31)
        )

        val expenses = listOf(
            Expense(
                id = 1L,
                amount = 700.0,
                categoryId = 1L,
                description = "Expense",
                date = dateMillis(2026, 10, 10)
            )
        )

        every { getExpenseUseCase() } returns flowOf(expenses)
        every { budgetUseCase.getBudgets() } returns flowOf(listOf(budget))

        viewModel = DashboardViewModel(
            getExpenseUseCase,
            budgetUseCase
        )

        viewModel.uiState.test {
            awaitItem()

            val state = awaitItem()

            assertEquals(500.0, state.totalBudget)
            assertEquals(700.0, state.totalBudgetSpent)
            assertEquals(-200.0, state.totalBudgetRemaining)
            assertEquals(1f, state.budgetProgress)
            assertEquals(true, state.isOverBudget)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun recentExpensesAreLimitedToFive() = runTest {
        val expenses = (1L..7L).map { id ->
            Expense(
                id = id,
                amount = id.toDouble(),
                categoryId = 1L,
                description = "Expense $id",
                date = dateMillis(2026, 10, id.toInt())
            )
        }

        every { getExpenseUseCase() } returns flowOf(expenses)

        viewModel = DashboardViewModel(
            getExpenseUseCase,
            budgetUseCase
        )

        viewModel.uiState.test {
            awaitItem()

            val state = awaitItem()

            assertEquals(5, state.recentExpenses.size)

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