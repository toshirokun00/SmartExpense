package com.smartexpense.dashoard

import app.cash.turbine.test
import com.smartexpense.dashoard.viewmodel.DashboardViewModel
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.usecase.GetExpenseUseCase
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class DashboardViewModelTest {

    private lateinit var getExpenseUseCase: GetExpenseUseCase
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setup() {
        getExpenseUseCase = mockk()
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
            getExpenseUseCase = getExpenseUseCase
        )

        viewModel.uiState.test {
            awaitItem()

            val state = awaitItem()

            assertEquals(
                425.75,
                state.totalAmount,
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


}