package com.smartexpense.feature.expenses

import app.cash.turbine.test
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.usecase.AddExpenseUseCase
import com.smartexpense.domain.usecase.DeleteExpenseUseCase
import com.smartexpense.domain.usecase.GetExpenseByIdUseCase
import com.smartexpense.domain.usecase.GetExpenseUseCase
import com.smartexpense.domain.usecase.UpdateExpenseUseCase
import com.smartexpense.domain.usecase.ValidateAmountUseCase
import com.smartexpense.feature.expenses.effect.ExpenseEffect
import com.smartexpense.feature.expenses.intent.ExpenseIntent
import com.smartexpense.feature.expenses.viewmodel.ExpenseViewModel
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExpenseViewModelTest {

    private lateinit var getExpensesUseCase: GetExpenseUseCase
    private lateinit var getExpenseByIdUseCase: GetExpenseByIdUseCase
    private lateinit var addExpenseUseCase: AddExpenseUseCase
    private lateinit var updateExpenseUseCase: UpdateExpenseUseCase
    private lateinit var deleteExpenseUseCase: DeleteExpenseUseCase
    private lateinit var validateExpenseAmountUseCase: ValidateAmountUseCase

    private lateinit var viewModel: ExpenseViewModel

    @Before
    fun setup() {
        getExpensesUseCase = mockk()
        getExpenseByIdUseCase = mockk()
        addExpenseUseCase = mockk()
        updateExpenseUseCase = mockk()
        deleteExpenseUseCase = mockk()
        validateExpenseAmountUseCase = mockk()

        every {
            getExpensesUseCase()
        } returns flowOf(emptyList())

        viewModel = ExpenseViewModel(
            getExpenseUseCase = getExpensesUseCase,
            getExpenseByIdUseCase = getExpenseByIdUseCase,
            addExpenseUseCase = addExpenseUseCase,
            updateExpenseUseCase = updateExpenseUseCase,
            deleteExpenseUseCase = deleteExpenseUseCase,
            validateExpenseAmountUseCase = validateExpenseAmountUseCase
        )

    }

    @Test
    fun amountChangedUpdatesFormState() = runTest {

        viewModel.uiState.test {

            awaitItem()
            viewModel.onIntent(
                ExpenseIntent.AmountChanged("100.50")
            )
            val state = awaitItem()
            assertEquals("100.50", state.form.amount)
        }
    }

    @Test
    fun descriptionChangedUpdatesFormState() = runTest {

        viewModel.uiState.test {

            awaitItem()
            viewModel.onIntent(
                ExpenseIntent.DescriptionChanged("Lunch")
            )

            val state = awaitItem()

            assertEquals("Lunch", state.form.description)
        }
    }


    @Test
    fun addExpenseWithValidAmountCallUseCase() = runTest {
        viewModel.onIntent(
            ExpenseIntent.AmountChanged("100.50")
        )

        viewModel.onIntent(
            ExpenseIntent.DescriptionChanged("Lunch")
        )

        every {
            validateExpenseAmountUseCase("100.50")
        } returns 100.50

        coEvery {
            addExpenseUseCase(any())
        } just Runs

        viewModel.effect.test {
            viewModel.onIntent(ExpenseIntent.AddExpenses)

            assertEquals(
                ExpenseEffect.ExpenseAdded, awaitItem()
            )
            coEvery {
                addExpenseUseCase(
                    match<Expense> {
                        it.amount == 100.50 &&
                                it.description == "Lunch"
                    }
                )
            }
        }


    }

    @Test
    fun addExpenseWithInvalidAmountShowsError() = runTest {
        viewModel.onIntent(
            ExpenseIntent.AmountChanged("abc")
        )
        every {
            validateExpenseAmountUseCase("abc")
        } returns null

        viewModel.effect.test {
            viewModel.onIntent(
                ExpenseIntent.AddExpenses
            )

            val effect = awaitItem()
            assertTrue(effect is ExpenseEffect.ShowError)

            coVerify(exactly = 0) {
                addExpenseUseCase(any())
            }
        }
    }

    @Test
    fun requestDeleteExpenseShowsConfirmation() = runTest {
        viewModel.onIntent(
            ExpenseIntent.RequestDeleteExpense(10L)
        )

        val state = viewModel.screenState.value

        assertTrue(state.showDeleteConfirmation)

        assertEquals(10L, state.expenseToDeleteId)
    }

    @Test
    fun cancelDeleteExpenseClearsConfirmation() = runTest {
        viewModel.onIntent(
            ExpenseIntent.RequestDeleteExpense(10L)
        )

        viewModel.onIntent(ExpenseIntent.CancelDeleteExpense)

        val state = viewModel.screenState.value

        assertFalse(state.showDeleteConfirmation)
        assertNull(state.expenseToDeleteId)
    }

    @Test
    fun confirmDeleteExpenseCallsDeleteUseCase() = runTest {
        val expense = Expense(
            id = 10L,
            amount = 100.0,
            categoryId = 1L,
            description = "Lunch",
            date = 123456L
        )

        every {
            getExpensesUseCase()
        } returns flowOf(listOf(expense))

        coEvery {
            deleteExpenseUseCase(expense)
        } just Runs

        viewModel = ExpenseViewModel(
            getExpenseUseCase = getExpensesUseCase,
            getExpenseByIdUseCase = getExpenseByIdUseCase,
            addExpenseUseCase = addExpenseUseCase,
            updateExpenseUseCase = updateExpenseUseCase,
            deleteExpenseUseCase = deleteExpenseUseCase,
            validateExpenseAmountUseCase =  validateExpenseAmountUseCase
        )

        viewModel.uiState.test {

            awaitItem()

            viewModel.effect.test {
                viewModel.onIntent(
                    ExpenseIntent.RequestDeleteExpense(10L)
                )

                viewModel.onIntent(
                    ExpenseIntent.ConfirmDeleteExpense
                )

                  val effect = awaitItem()
                assertEquals(ExpenseEffect.ExpenseDeleted, effect)
            }

            cancelAndIgnoreRemainingEvents()
        }

        coVerify {
            deleteExpenseUseCase(expense)
        }

        assertFalse(
            viewModel.screenState.value.showDeleteConfirmation
        )

        assertNull(viewModel.screenState.value.expenseToDeleteId)
    }


}