package com.smartexpense.feature.expenses

import app.cash.turbine.test
import com.smartexpense.domain.model.AppSettings
import com.smartexpense.domain.model.AppTheme
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.model.ExpenseSyncStatus
import com.smartexpense.domain.usecase.AddExpenseUseCase
import com.smartexpense.domain.usecase.CategoryUseCase
import com.smartexpense.domain.usecase.DeleteExpenseUseCase
import com.smartexpense.domain.usecase.GetExpenseByIdUseCase
import com.smartexpense.domain.usecase.GetExpenseUseCase
import com.smartexpense.domain.usecase.ScheduleSyncUseCase
import com.smartexpense.domain.usecase.SettingsUseCase
import com.smartexpense.domain.usecase.UpdateExpenseUseCase
import com.smartexpense.domain.usecase.ValidateAmountUseCase
import com.smartexpense.feature.expenses.effect.ExpenseEffect
import com.smartexpense.feature.expenses.intent.ExpenseIntent
import com.smartexpense.feature.expenses.viewmodel.ExpenseViewModel
import com.smartexpense.notification.ExpenseNotificationManager
import com.smartexpense.test.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var getExpensesUseCase: GetExpenseUseCase
    private lateinit var getExpenseByIdUseCase: GetExpenseByIdUseCase
    private lateinit var addExpenseUseCase: AddExpenseUseCase
    private lateinit var updateExpenseUseCase: UpdateExpenseUseCase
    private lateinit var deleteExpenseUseCase: DeleteExpenseUseCase

    private lateinit var categoryUseCase: CategoryUseCase
    private lateinit var validateExpenseAmountUseCase: ValidateAmountUseCase

    private lateinit var settingsUseCase: SettingsUseCase

    private lateinit var scheduleSyncUseCase: ScheduleSyncUseCase

    private lateinit var expenseNotificationManager: ExpenseNotificationManager

    private lateinit var viewModel: ExpenseViewModel

    @Before
    fun setup() {
        getExpensesUseCase = mockk()
        getExpenseByIdUseCase = mockk()
        addExpenseUseCase = mockk()
        updateExpenseUseCase = mockk()
        deleteExpenseUseCase = mockk()
        validateExpenseAmountUseCase = mockk()
        categoryUseCase = mockk()
        expenseNotificationManager = mockk()
        settingsUseCase = mockk()
        scheduleSyncUseCase = mockk(relaxed = true)

        every {
            expenseNotificationManager.showExpenseAddedNotification(
                description = any(),
                amount = any()
            )
        } returns true

        every {
            getExpensesUseCase()
        } returns flowOf(emptyList())

        every {
            categoryUseCase.getCategories()
        } returns flowOf(emptyList())

        every {
            settingsUseCase.getSettings()
        } returns flowOf(
            AppSettings(
                theme = AppTheme.SYSTEM,
                notificationsEnabled = true
            )
        )

        viewModel = ExpenseViewModel(
            getExpenseUseCase = getExpensesUseCase,
            getExpenseByIdUseCase = getExpenseByIdUseCase,
            addExpenseUseCase = addExpenseUseCase,
            updateExpenseUseCase = updateExpenseUseCase,
            deleteExpenseUseCase = deleteExpenseUseCase,
            validateExpenseAmountUseCase = validateExpenseAmountUseCase,
            categoryUseCase = categoryUseCase,
            settingsUseCase = settingsUseCase,
            scheduleSyncUseCase = scheduleSyncUseCase,
            expenseNotificationManager = expenseNotificationManager
        )
    }

    @After
    fun tearDown() {
        // No-op: ViewModel.onCleared() is protected and cannot be called
        // from tests. MainDispatcherRule handles dispatcher reset between
        // tests. Each @Before creates a fresh ExpenseViewModel instance,
        // so viewModelScope is per-test and does not leak.
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
    fun amountChangedFormatsAmountWithCommasWhileTyping() = runTest {
        viewModel.uiState.test {
            awaitItem()

            viewModel.onIntent(ExpenseIntent.AmountChanged("1234"))
            assertEquals("1,234", awaitItem().form.amount)

            viewModel.onIntent(ExpenseIntent.AmountChanged("12345"))
            assertEquals("12,345", awaitItem().form.amount)

            viewModel.onIntent(ExpenseIntent.AmountChanged("12,345.5"))
            assertEquals("12,345.5", awaitItem().form.amount)

            viewModel.onIntent(ExpenseIntent.AmountChanged("12,345.50"))
            assertEquals("12,345.50", awaitItem().form.amount)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun descriptionChangedUpdatesFormState() = runTest {
        viewModel.uiState.test {
            // Consume the initial state.
            awaitItem()

            viewModel.onIntent(
                ExpenseIntent.DescriptionChanged("Lunch")
            )

            val updatedState = awaitItem()

            assertEquals("Lunch", updatedState.form.description)
        }
    }

    @Test
    fun addExpenseWithValidAmountCallUseCase() = runTest {
        every {
            validateExpenseAmountUseCase("100.50")
        } returns 100.50
        coEvery {
            addExpenseUseCase(any())
        } just Runs

        viewModel.onIntent(
            ExpenseIntent.AmountChanged("100.50")
        )
        viewModel.onIntent(
            ExpenseIntent.DescriptionChanged("Lunch")
        )
        viewModel.onIntent(ExpenseIntent.CategoryChanged(1L))
        viewModel.onIntent(ExpenseIntent.AddExpenses)

        advanceUntilIdle()

        coVerify(exactly = 1) {
            addExpenseUseCase(
                match {
                    it.amount == 100.50 &&
                        it.description == "Lunch" &&
                        it.categoryId == 1L
                }
            )
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

        every { getExpensesUseCase() } returns flowOf(listOf(expense))
        coEvery { deleteExpenseUseCase(expense) } just Runs

        viewModel = ExpenseViewModel(
            getExpenseUseCase = getExpensesUseCase,
            getExpenseByIdUseCase = getExpenseByIdUseCase,
            addExpenseUseCase = addExpenseUseCase,
            updateExpenseUseCase = updateExpenseUseCase,
            deleteExpenseUseCase = deleteExpenseUseCase,
            validateExpenseAmountUseCase = validateExpenseAmountUseCase,
            categoryUseCase = categoryUseCase,
            settingsUseCase = settingsUseCase,
            scheduleSyncUseCase = scheduleSyncUseCase,
            expenseNotificationManager = expenseNotificationManager
        )

        viewModel.uiState.test {
            // Wait until the expense flow has populated uiState.
            var state = awaitItem()
            while (expense !in state.expenses) {
                state = awaitItem()
            }

            viewModel.effect.test {
                viewModel.onIntent(
                    ExpenseIntent.RequestDeleteExpense(10L)
                )
                viewModel.onIntent(
                    ExpenseIntent.ConfirmDeleteExpense
                )

                assertEquals(
                    ExpenseEffect.ExpenseDeleted,
                    awaitItem()
                )

                coVerify(exactly = 1) {
                    deleteExpenseUseCase(expense)
                }

                cancelAndIgnoreRemainingEvents()
            }
            cancelAndIgnoreRemainingEvents()
        }

        assertFalse(viewModel.screenState.value.showDeleteConfirmation)
        assertNull(viewModel.screenState.value.expenseToDeleteId)
    }

    @Test
    fun totalAmountIsCalculatedFromExpenses() = runTest {
        val expenses = listOf(
            Expense(id = 1L, amount = 100.0, categoryId = 1L, description = "Lunch", date = 1L),
            Expense(id = 2L, amount = 250.50, categoryId = 1L, description = "Groceries", date = 2L),
            Expense(id = 3L, amount = 75.25, categoryId = 2L, description = "Transport", date = 3L)
        )

        every { getExpensesUseCase() } returns flowOf(expenses)

        viewModel = ExpenseViewModel(
            getExpenseUseCase = getExpensesUseCase,
            addExpenseUseCase = addExpenseUseCase,
            updateExpenseUseCase = updateExpenseUseCase,
            deleteExpenseUseCase = deleteExpenseUseCase,
            getExpenseByIdUseCase = getExpenseByIdUseCase,
            validateExpenseAmountUseCase = validateExpenseAmountUseCase,
            categoryUseCase = categoryUseCase,
            settingsUseCase = settingsUseCase,
            scheduleSyncUseCase = scheduleSyncUseCase,
            expenseNotificationManager = expenseNotificationManager
        )

        viewModel.uiState.test {
            awaitItem()
            val state = awaitItem()
            assertEquals(425.75, state.totalAmount, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addExpenseShowsNotificationWhenEnabled() = runTest {
        every { validateExpenseAmountUseCase("100") } returns 100.0
        coEvery { addExpenseUseCase(any()) } just Runs

        viewModel.onIntent(ExpenseIntent.AmountChanged("100"))
        viewModel.onIntent(ExpenseIntent.CategoryChanged(1L))
        viewModel.onIntent(ExpenseIntent.AddExpenses)

        advanceUntilIdle()

        verify {
            expenseNotificationManager.showExpenseAddedNotification(
                description = any(),
                amount = "₱100.00"
            )
        }
    }

    @Test
    fun addExpenseDoesNotShowNotificationWhenDisabled() = runTest {
        every { settingsUseCase.getSettings() } returns flowOf(
            AppSettings(theme = AppTheme.SYSTEM, notificationsEnabled = false)
        )
        every { validateExpenseAmountUseCase("100") } returns 100.0
        coEvery { addExpenseUseCase(any()) } just Runs

        viewModel = ExpenseViewModel(
            getExpenseUseCase = getExpensesUseCase,
            getExpenseByIdUseCase = getExpenseByIdUseCase,
            addExpenseUseCase = addExpenseUseCase,
            updateExpenseUseCase = updateExpenseUseCase,
            deleteExpenseUseCase = deleteExpenseUseCase,
            validateExpenseAmountUseCase = validateExpenseAmountUseCase,
            categoryUseCase = categoryUseCase,
            settingsUseCase = settingsUseCase,
            scheduleSyncUseCase = scheduleSyncUseCase,
            expenseNotificationManager = expenseNotificationManager
        )

        viewModel.onIntent(ExpenseIntent.AmountChanged("100"))
        viewModel.onIntent(ExpenseIntent.CategoryChanged(1L))
        viewModel.onIntent(ExpenseIntent.AddExpenses)

        advanceUntilIdle()

        verify(exactly = 0) {
            expenseNotificationManager.showExpenseAddedNotification(
                description = any(),
                amount = any()
            )
        }
    }

    @Test
    fun addExpenseDoesNotShowNotificationWhenSaveFails() = runTest {
        every { validateExpenseAmountUseCase("100") } returns 100.0
        coEvery { addExpenseUseCase(any()) } throws IllegalStateException("Save failed")

        viewModel.onIntent(ExpenseIntent.AmountChanged("100"))
        viewModel.onIntent(ExpenseIntent.CategoryChanged(1L))
        viewModel.onIntent(ExpenseIntent.AddExpenses)

        advanceUntilIdle()

        verify(exactly = 0) {
            expenseNotificationManager.showExpenseAddedNotification(
                description = any(),
                amount = any()
            )
        }
    }

    @Test
    fun addExpenseFailureKeepsFormAndReportsOnlyTheError() = runTest {
        every { validateExpenseAmountUseCase("100") } returns 100.0
        coEvery { addExpenseUseCase(any()) } throws IllegalStateException("Save failed")

        viewModel.uiState.test {
            awaitItem()

            viewModel.onIntent(ExpenseIntent.AmountChanged("100"))
            viewModel.onIntent(ExpenseIntent.DescriptionChanged("Lunch"))
            viewModel.onIntent(ExpenseIntent.MerchantChanged("Cafe Uno"))
            viewModel.onIntent(ExpenseIntent.CategoryChanged(1L))

            viewModel.effect.test {
                viewModel.onIntent(ExpenseIntent.AddExpenses)
                advanceUntilIdle()

                assertTrue(awaitItem() is ExpenseEffect.ShowError)
                cancelAndIgnoreRemainingEvents()
            }

            val state = viewModel.uiState.value
            assertEquals("100", state.form.amount)
            assertEquals("Lunch", state.form.description)
            assertEquals("Cafe Uno", state.form.merchant)
            assertFalse(state.form.isSaving)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun merchantAndDateChangesUpdateFormState() = runTest {
        viewModel.uiState.test {
            awaitItem()

            viewModel.onIntent(ExpenseIntent.MerchantChanged("Cafe Uno"))
            assertEquals("Cafe Uno", awaitItem().form.merchant)

            viewModel.onIntent(ExpenseIntent.DateChanged(1_700_000_000_000L))
            assertEquals(1_700_000_000_000L, awaitItem().form.date)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loadExpensePopulatesDateMerchantAndSyncStatus() = runTest {
        val expense = Expense(
            id = 1L,
            amount = 123.45,
            categoryId = 2L,
            description = "Coffee",
            date = 1_700_000_000_000L,
            merchant = "Cafe Uno",
            syncStatus = ExpenseSyncStatus.FAILED
        )
        coEvery { getExpenseByIdUseCase(1L) } returns expense

        viewModel.uiState.test {
            awaitItem()

            viewModel.onIntent(ExpenseIntent.LoadExpense(1L))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("123.45", state.form.amount)
            assertEquals("Cafe Uno", state.form.merchant)
            assertEquals(1_700_000_000_000L, state.form.date)
            assertEquals(ExpenseSyncStatus.FAILED, state.form.syncStatus)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun retrySyncRequestsBackgroundSync() = runTest {
        viewModel.onIntent(ExpenseIntent.RetrySync(1L))
        advanceUntilIdle()

        verify { scheduleSyncUseCase() }
    }
}
