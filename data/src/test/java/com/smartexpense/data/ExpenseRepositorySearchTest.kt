package com.smartexpense.data

import com.smartexpense.data.repositoryimpl.ExpenseRepositoryImpl
import com.smartexpense.database.dao.ExpenseDao
import com.smartexpense.database.entity.ExpenseEntity
import com.smartexpense.domain.model.SearchCriteria
import com.smartexpense.domain.model.SortOrder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ExpenseRepositorySearchTest {

    private lateinit var expenseDao: ExpenseDao
    private lateinit var repository: ExpenseRepositoryImpl

    @Before
    fun setup() {
        expenseDao = mockk(relaxed = true)
        repository = ExpenseRepositoryImpl(
            expenseDao = expenseDao,
            supabaseExpenseRemoteDataSource = mockk(relaxed = true),
            supabaseClient = mockk(relaxed = true),
            categoryRepository = mockk(relaxed = true),
            expenseSyncEnqueuer = mockk(relaxed = true),
            pendingExpenseDeletionDao = mockk(relaxed = true),
            deletionTransaction = mockk(relaxed = true),
            remoteDataSync = mockk(relaxed = true)
        )
    }

    @Test
    fun searchExpensesMapsDaoResultToDomain() = runTest {
        val criteria = SearchCriteria(
            query = "lunch",
            selectedCategoryIds = setOf(1L),
            minAmount = 10.0,
            maxAmount = 50.0,
            sortOrder = SortOrder.NEWEST
        )
        val entities = listOf(
            ExpenseEntity(
                id = 1L,
                amount = 25.0,
                categoryId = 1L,
                description = "Lunch",
                date = 1_760_000_000_000L,
                syncStatus = com.smartexpense.database.entity.SyncStatus.SYNCED,
                clientId = "client-1"
            )
        )

        coEvery { expenseDao.searchExpenses(
            query = "lunch",
            categoryIds = listOf(1L),
            categoryIdCount = 1,
            minAmount = 10.0,
            maxAmount = 50.0,
            startTime = null,
            endTime = null,
            sortOrder = "NEWEST"
        ) } returns flowOf(entities)

        val result = repository.searchExpenses(criteria).first()

        assertEquals(
            listOf(
                com.smartexpense.domain.model.Expense(
                    id = 1L,
                    amount = 25.0,
                    categoryId = 1L,
                    description = "Lunch",
                    date = 1_760_000_000_000L
                )
            ),
            result
        )
    }

    @Test
    fun searchExpensesPassesNormalizedCriteria() = runTest {
        val criteria = SearchCriteria(
            query = "  Lunch  ",
            selectedCategoryIds = setOf(1L, 2L),
            minAmount = null,
            maxAmount = null,
            startTime = null,
            endTime = null,
            sortOrder = SortOrder.HIGHEST_AMOUNT
        )
        val entities = emptyList<ExpenseEntity>()

        coEvery { expenseDao.searchExpenses(
            query = "Lunch",
            categoryIds = listOf(1L, 2L),
            categoryIdCount = 2,
            minAmount = null,
            maxAmount = null,
            startTime = null,
            endTime = null,
            sortOrder = "HIGHEST_AMOUNT"
        ) } returns flowOf(entities)

        repository.searchExpenses(criteria).first()

        coVerify(exactly = 1) { expenseDao.searchExpenses(
            query = "Lunch",
            categoryIds = listOf(1L, 2L),
            categoryIdCount = 2,
            minAmount = null,
            maxAmount = null,
            startTime = null,
            endTime = null,
            sortOrder = "HIGHEST_AMOUNT"
        )}
    }

    @Test
    fun searchExpensesWithEmptyCriteriaReturnsAll() = runTest {
        val criteria = SearchCriteria()
        val entities = listOf(
            ExpenseEntity(
                id = 1L,
                amount = 10.0,
                categoryId = 1L,
                description = "Item",
                date = 1_760_000_000_000L,
                syncStatus = com.smartexpense.database.entity.SyncStatus.SYNCED,
                clientId = "client-1"
            )
        )

        coEvery { expenseDao.searchExpenses(
            query = null,
            categoryIds = emptyList(),
            categoryIdCount = 0,
            minAmount = null,
            maxAmount = null,
            startTime = null,
            endTime = null,
            sortOrder = "NEWEST"
        ) } returns flowOf(entities)

        val result = repository.searchExpenses(criteria).first()

        assertEquals(1, result.size)
    }
}
