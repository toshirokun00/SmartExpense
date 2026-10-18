package com.smartexpense.feature.expenses

import app.cash.turbine.test
import com.smartexpense.domain.model.SearchCriteria
import com.smartexpense.domain.model.SortOrder
import com.smartexpense.domain.usecase.CategoryUseCase
import com.smartexpense.domain.usecase.SearchExpensesUseCase
import com.smartexpense.feature.expenses.effect.TransactionEffect
import com.smartexpense.feature.expenses.intent.TransactionIntent
import com.smartexpense.feature.expenses.state.TransactionUiState
import com.smartexpense.feature.expenses.viewmodel.TransactionViewModel
import com.smartexpense.test.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var searchExpensesUseCase: SearchExpensesUseCase
    private lateinit var categoryUseCase: CategoryUseCase
    private lateinit var viewModel: TransactionViewModel

    @Before
    fun setup() {
        searchExpensesUseCase = mockk()
        categoryUseCase = mockk()

        every { categoryUseCase.getCategories() } returns flowOf(emptyList())
        coEvery { searchExpensesUseCase(any()) } returns flowOf(emptyList())

        viewModel = TransactionViewModel(
            searchExpensesUseCase = searchExpensesUseCase,
            categoryUseCase = categoryUseCase
        )
    }

    @Test
    fun initialStateHasDefaultSortingAndEmptyResults() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(SortOrder.NEWEST, state.sortOrder)
            assertTrue(state.expenses.isEmpty())
            assertTrue(state.query.isEmpty())
            assertTrue(state.selectedCategoryIds.isEmpty())
            assertNull(state.startTime)
            assertNull(state.endTime)
            assertNull(state.minAmount)
            assertNull(state.maxAmount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun searchChangedUpdatesQueryAndTriggersSearch() = runTest {
        val expenses = listOf(
            com.smartexpense.domain.model.Expense(
                id = 1L,
                amount = 10.0,
                categoryId = 1L,
                description = "Coffee",
                date = 1_760_000_000_000L
            )
        )
        coEvery { searchExpensesUseCase(any()) } returns flowOf(expenses)

        viewModel.uiState.test {
            awaitItem() // initial loading state

            viewModel.onIntent(
                TransactionIntent.SearchChanged("Coffee")
            )

            advanceUntilIdle()

            val state = awaitItem()
            assertEquals("Coffee", state.query)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) {
            searchExpensesUseCase(
                match { it.query == "Coffee" }
            )
        }
    }

    @Test
    fun categorySelectionChangedUpdatesSelectedCategories() = runTest {
        coEvery { searchExpensesUseCase(any()) } returns flowOf(emptyList())

        viewModel.uiState.test {
            awaitItem()

            viewModel.onIntent(
                TransactionIntent.CategorySelectionChanged(setOf(1L, 2L))
            )

            advanceUntilIdle()

            val state = awaitItem()
            assertEquals(setOf(1L, 2L), state.selectedCategoryIds)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) {
            searchExpensesUseCase(
                match { it.selectedCategoryIds == setOf(1L, 2L) }
            )
        }
    }

    @Test
    fun dateRangeChangedUpdatesStartAndEndTime() = runTest {
        coEvery { searchExpensesUseCase(any()) } returns flowOf(emptyList())

        viewModel.uiState.test {
            awaitItem()

            viewModel.onIntent(
                TransactionIntent.DateRangeChanged(
                    startTime = 1_760_000_000_000L,
                    endTime = 1_760_086_400_000L
                )
            )

            advanceUntilIdle()

            val state = awaitItem()
            assertEquals(1_760_000_000_000L, state.startTime)
            assertEquals(1_760_086_400_000L, state.endTime)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun amountRangeChangedUpdatesMinAndMax() = runTest {
        coEvery { searchExpensesUseCase(any()) } returns flowOf(emptyList())

        viewModel.uiState.test {
            awaitItem()

            viewModel.onIntent(
                TransactionIntent.AmountRangeChanged(
                    minAmount = 10.0,
                    maxAmount = 100.0
                )
            )

            advanceUntilIdle()

            val state = awaitItem()
            assertEquals(10.0, state.minAmount)
            assertEquals(100.0, state.maxAmount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun sortChangedUpdatesSortOrder() = runTest {
        coEvery { searchExpensesUseCase(any()) } returns flowOf(emptyList())

        viewModel.uiState.test {
            awaitItem()

            viewModel.onIntent(
                TransactionIntent.SortChanged(SortOrder.HIGHEST_AMOUNT)
            )

            advanceUntilIdle()

            val state = awaitItem()
            assertEquals(SortOrder.HIGHEST_AMOUNT, state.sortOrder)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun clearFiltersResetsAllCriteria() = runTest {
        coEvery { searchExpensesUseCase(any()) } returns flowOf(emptyList())

        // First set some filters
        viewModel.onIntent(
            TransactionIntent.SearchChanged("Coffee")
        )
        viewModel.onIntent(
            TransactionIntent.CategorySelectionChanged(setOf(1L))
        )
        viewModel.onIntent(
            TransactionIntent.SortChanged(SortOrder.OLDEST)
        )

        advanceUntilIdle()

        // Then clear
        viewModel.uiState.test {
            viewModel.onIntent(TransactionIntent.ClearFilters)

            advanceUntilIdle()

            val state = awaitItem()
            assertEquals("", state.query)
            assertTrue(state.selectedCategoryIds.isEmpty())
            assertEquals(SortOrder.NEWEST, state.sortOrder)
            assertNull(state.startTime)
            assertNull(state.endTime)
            assertNull(state.minAmount)
            assertNull(state.maxAmount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun combinedCriteriaAreAllPassedToSearch() = runTest {
        coEvery { searchExpensesUseCase(any()) } returns flowOf(emptyList())

        viewModel.uiState.test {
            awaitItem()

            viewModel.onIntent(
                TransactionIntent.SearchChanged("Coffee")
            )
            viewModel.onIntent(
                TransactionIntent.CategorySelectionChanged(setOf(1L))
            )
            viewModel.onIntent(
                TransactionIntent.DateRangeChanged(
                    startTime = 1_760_000_000_000L,
                    endTime = null
                )
            )
            viewModel.onIntent(
                TransactionIntent.AmountRangeChanged(
                    minAmount = 5.0,
                    maxAmount = null
                )
            )
            viewModel.onIntent(
                TransactionIntent.SortChanged(SortOrder.HIGHEST_AMOUNT)
            )

            advanceUntilIdle()

            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) {
            searchExpensesUseCase(
                match {
                    it.query == "Coffee" &&
                        it.selectedCategoryIds == setOf(1L) &&
                        it.startTime == 1_760_000_000_000L &&
                        it.endTime == null &&
                        it.minAmount == 5.0 &&
                        it.maxAmount == null &&
                        it.sortOrder == SortOrder.HIGHEST_AMOUNT
                }
            )
        }
    }

    @Test
    fun errorIsEmittedWhenSearchFails() = runTest {
        coEvery { searchExpensesUseCase(any()) } throws RuntimeException("Database error")

        viewModel.effect.test {
            viewModel.onIntent(
                TransactionIntent.SearchChanged("Anything")
            )


            val effect = awaitItem()
            assertTrue(effect is TransactionEffect.ShowError)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
