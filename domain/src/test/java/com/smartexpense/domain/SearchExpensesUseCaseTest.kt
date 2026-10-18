package com.smartexpense.domain

import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.model.SearchCriteria
import com.smartexpense.domain.model.SortOrder
import com.smartexpense.domain.repository.ExpenseRepository
import com.smartexpense.domain.usecase.SearchExpensesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SearchExpensesUseCaseTest {

    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var useCase: SearchExpensesUseCase

    @Before
    fun setup() {
        expenseRepository = mockk()
        useCase = SearchExpensesUseCase(expenseRepository)
    }

    @Test
    fun passesCriteriaToRepositoryAndReturnsResult() = runTest {
        val criteria = SearchCriteria(
            query = "coffee",
            selectedCategoryIds = setOf(1L),
            minAmount = 10.0,
            maxAmount = 100.0,
            sortOrder = SortOrder.HIGHEST_AMOUNT
        )
        val expectedExpenses = listOf(
            Expense(id = 1L, amount = 50.0, categoryId = 1L, description = "Coffee", date = 1_760_000_000_000L)
        )

        coEvery { expenseRepository.searchExpenses(criteria) } returns flowOf(expectedExpenses)

        val result = useCase(criteria).first()

        assertEquals(expectedExpenses, result)
        coVerify(exactly = 1) { expenseRepository.searchExpenses(criteria) }
    }

    @Test
    fun passesDefaultCriteriaWhenEmpty() = runTest {
        val criteria = SearchCriteria()
        val expectedExpenses = emptyList<Expense>()

        coEvery { expenseRepository.searchExpenses(criteria) } returns flowOf(expectedExpenses)

        val result = useCase(criteria).first()

        assertEquals(expectedExpenses, result)
        coVerify(exactly = 1) { expenseRepository.searchExpenses(criteria) }
    }
}
