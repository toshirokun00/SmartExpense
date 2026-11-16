package com.smartexpense.paycheck

import com.smartexpense.domain.model.Bill
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.model.Paycheck
import com.smartexpense.domain.usecase.GetBillsUseCase
import com.smartexpense.domain.usecase.GetExpenseUseCase
import com.smartexpense.domain.usecase.GetPaychecksUseCase
import com.smartexpense.paycheck.viewmodel.MoneyTrackerViewModel
import com.smartexpense.test.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class MoneyTrackerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(
        Instant.parse("2026-11-13T12:00:00Z"),
        ZoneOffset.UTC
    )

    private fun dateMillis(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

    private fun expenseMillis(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day)
            .atTime(12, 0)
            .atZone(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

    @Test
    fun `builds envelopes and totals from paychecks bills and expenses`() =
        runTest {
            val paychecks = MutableStateFlow(
                listOf(
                    Paycheck(
                        id = 1L,
                        amount = 1_000.0,
                        payDateMillis = dateMillis(2026, 11, 15),
                        billIds = listOf(1L)
                    ),
                    Paycheck(
                        id = 2L,
                        amount = 1_000.0,
                        payDateMillis = dateMillis(2026, 11, 30)
                    )
                )
            )
            val bills = MutableStateFlow(
                listOf(
                    Bill(
                        id = 1L,
                        name = "Rent",
                        amount = 200.0,
                        dueDateMillis = dateMillis(2026, 11, 10)
                    )
                )
            )
            val expenses = MutableStateFlow(
                listOf(
                    expense(2026, 11, 14, 30.0),
                    expense(2026, 11, 16, 100.0),
                    expense(2026, 11, 20, 50.0),
                    expense(2026, 12, 1, 25.0)
                )
            )
            val getPaychecksUseCase = mockk<GetPaychecksUseCase>()
            val getBillsUseCase = mockk<GetBillsUseCase>()
            val getExpenseUseCase = mockk<GetExpenseUseCase>()
            every { getPaychecksUseCase() } returns paychecks
            every { getBillsUseCase() } returns bills
            every { getExpenseUseCase() } returns expenses

            val viewModel = MoneyTrackerViewModel(
                getPaychecksUseCase = getPaychecksUseCase,
                getBillsUseCase = getBillsUseCase,
                getExpenseUseCase = getExpenseUseCase,
                clock = clock
            )
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(2, state.envelopes.size)
            assertEquals(2_000.0, state.totalIncome, 0.001)
            assertEquals(200.0, state.totalBills, 0.001)
            assertEquals(175.0, state.totalExpenses, 0.001)
            assertEquals(1_625.0, state.totalRemaining, 0.001)

            val first = state.envelopes.first()
            assertEquals(650.0, first.remaining, 0.001)
            assertEquals(listOf(100.0, 50.0), first.expenses.map { it.amount })

            val second = state.envelopes.last()
            assertEquals(975.0, second.remaining, 0.001)
        }

    private fun expense(year: Int, month: Int, day: Int, amount: Double) =
        Expense(
            amount = amount,
            categoryId = 1L,
            description = "Expense",
            date = expenseMillis(year, month, day)
        )
}
