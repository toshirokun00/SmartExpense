package com.smartexpense.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartexpense.database.dao.ExpenseDao
import com.smartexpense.database.entity.ExpenseEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class ExpenseSearchDaoTest {

    private companion object {
        const val AMOUNT_DELTA = 0.001
    }

    private val ExpenseEntity.amount: Double
        get() = amountMinor / 100.0

    private lateinit var database: AppDatabase
    private lateinit var expenseDao: ExpenseDao

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        expenseDao = database.expenseDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun insertExpense(
        id: Long = 0L,
        amount: Double,
        categoryId: Long,
        description: String,
        date: Long,
        merchant: String = ""
    ) = runBlocking {
        expenseDao.insertExpense(
            ExpenseEntity(
                id = id,
                amountMinor = kotlin.math.round(amount * 100).toLong(),
                categoryId = categoryId,
                description = description,
                date = date,
                merchant = merchant,
                syncStatus = com.smartexpense.database.entity.SyncStatus.SYNCED,
                clientId = "test-client-$id"
            )
        )
    }

    @Test
    fun searchByDescriptionIsCaseInsensitive() = runBlocking {
        insertExpense(amount = 10.0, categoryId = 1L, description = "Coffee Shop", date = 1_760_000_000_000L)
        insertExpense(amount = 20.0, categoryId = 1L, description = "coffee beans", date = 1_760_000_000_000L)
        insertExpense(amount = 30.0, categoryId = 1L, description = "Tea Shop", date = 1_760_000_000_000L)

        val results = expenseDao.searchExpenses(
            query = "COFFEE",
            categoryIds = emptyList(),
            categoryIdCount = 0,
            minAmountMinor = null,
            maxAmountMinor = null,
            startTime = null,
            endTime = null,
            sortOrder = "NEWEST"
        ).first()

        assertEquals(2, results.size)
        assertTrue(results.all { it.description.lowercase().contains("coffee") })
    }

    @Test
    fun searchByMerchantIsCaseInsensitive() = runBlocking {
        insertExpense(
            amount = 10.0,
            categoryId = 1L,
            description = "Latte",
            date = 1_760_000_000_000L,
            merchant = "Cafe Uno"
        )
        insertExpense(
            amount = 20.0,
            categoryId = 1L,
            description = "Beans",
            date = 1_760_000_000_000L,
            merchant = "Coffee Bean Co"
        )
        insertExpense(
            amount = 30.0,
            categoryId = 1L,
            description = "Sandwich",
            date = 1_760_000_000_000L,
            merchant = "Deli"
        )

        val results = expenseDao.searchExpenses(
            query = "coffee",
            categoryIds = emptyList(),
            categoryIdCount = 0,
            minAmountMinor = null,
            maxAmountMinor = null,
            startTime = null,
            endTime = null,
            sortOrder = "NEWEST"
        ).first()

        assertEquals(1, results.size)
        assertEquals("Coffee Bean Co", results.first().merchant)
    }

    @Test
    fun emptySearchQueryReturnsAllExpenses() = runBlocking {
        insertExpense(amount = 10.0, categoryId = 1L, description = "Item A", date = 1_760_000_000_000L)
        insertExpense(amount = 20.0, categoryId = 1L, description = "Item B", date = 1_760_001_000_000L)

        val results = expenseDao.searchExpenses(
            query = "",
            categoryIds = emptyList(),
            categoryIdCount = 0,
            minAmountMinor = null,
            maxAmountMinor = null,
            startTime = null,
            endTime = null,
            sortOrder = "NEWEST"
        ).first()

        assertEquals(2, results.size)
    }

    @Test
    fun nullSearchQueryReturnsAllExpenses() = runBlocking {
        insertExpense(amount = 10.0, categoryId = 1L, description = "Item A", date = 1_760_000_000_000L)

        val results = expenseDao.searchExpenses(
            query = null,
            categoryIds = emptyList(),
            categoryIdCount = 0,
            minAmountMinor = null,
            maxAmountMinor = null,
            startTime = null,
            endTime = null,
            sortOrder = "NEWEST"
        ).first()

        assertEquals(1, results.size)
    }

    @Test
    fun filterBySingleCategory() = runBlocking {
        insertExpense(amount = 10.0, categoryId = 1L, description = "Food A", date = 1_760_000_000_000L)
        insertExpense(amount = 20.0, categoryId = 2L, description = "Transport A", date = 1_760_000_000_000L)

        val results = expenseDao.searchExpenses(
            query = null,
            categoryIds = listOf(1L),
            categoryIdCount = 1,
            minAmountMinor = null,
            maxAmountMinor = null,
            startTime = null,
            endTime = null,
            sortOrder = "NEWEST"
        ).first()

        assertEquals(1, results.size)
        assertEquals(1L, results.first().categoryId)
    }

    @Test
    fun filterByMultipleCategories() = runBlocking {
        insertExpense(amount = 10.0, categoryId = 1L, description = "Food A", date = 1_760_000_000_000L)
        insertExpense(amount = 20.0, categoryId = 2L, description = "Transport A", date = 1_760_000_000_000L)
        insertExpense(amount = 30.0, categoryId = 3L, description = "Entertainment A", date = 1_760_000_000_000L)

        val results = expenseDao.searchExpenses(
            query = null,
            categoryIds = listOf(1L, 3L),
            categoryIdCount = 2,
            minAmountMinor = null,
            maxAmountMinor = null,
            startTime = null,
            endTime = null,
            sortOrder = "NEWEST"
        ).first()

        assertEquals(2, results.size)
        assertTrue(results.all { it.categoryId in listOf(1L, 3L) })
    }

    @Test
    fun filterByMinimumAmountOnly() = runBlocking {
        insertExpense(amount = 10.0, categoryId = 1L, description = "Cheap", date = 1_760_000_000_000L)
        insertExpense(amount = 50.0, categoryId = 1L, description = "Medium", date = 1_760_000_000_000L)
        insertExpense(amount = 100.0, categoryId = 1L, description = "Expensive", date = 1_760_000_000_000L)

        val results = expenseDao.searchExpenses(
            query = null,
            categoryIds = emptyList(),
            categoryIdCount = 0,
            minAmountMinor = 5000L,
            maxAmountMinor = null,
            startTime = null,
            endTime = null,
            sortOrder = "NEWEST"
        ).first()

        assertEquals(2, results.size)
        assertTrue(results.all { it.amount >= 50.0 })
    }

    @Test
    fun filterByMaximumAmountOnly() = runBlocking {
        insertExpense(amount = 10.0, categoryId = 1L, description = "Cheap", date = 1_760_000_000_000L)
        insertExpense(amount = 50.0, categoryId = 1L, description = "Medium", date = 1_760_000_000_000L)
        insertExpense(amount = 100.0, categoryId = 1L, description = "Expensive", date = 1_760_000_000_000L)

        val results = expenseDao.searchExpenses(
            query = null,
            categoryIds = emptyList(),
            categoryIdCount = 0,
            minAmountMinor = null,
            maxAmountMinor = 5000L,
            startTime = null,
            endTime = null,
            sortOrder = "NEWEST"
        ).first()

        assertEquals(2, results.size)
        assertTrue(results.all { it.amount <= 50.0 })
    }

    @Test
    fun filterByCombinedAmountRange() = runBlocking {
        insertExpense(amount = 10.0, categoryId = 1L, description = "Cheap", date = 1_760_000_000_000L)
        insertExpense(amount = 50.0, categoryId = 1L, description = "Medium", date = 1_760_000_000_000L)
        insertExpense(amount = 100.0, categoryId = 1L, description = "Expensive", date = 1_760_000_000_000L)

        val results = expenseDao.searchExpenses(
            query = null,
            categoryIds = emptyList(),
            categoryIdCount = 0,
            minAmountMinor = 2000L,
            maxAmountMinor = 8000L,
            startTime = null,
            endTime = null,
            sortOrder = "NEWEST"
        ).first()

        assertEquals(1, results.size)
        assertEquals(50.0, results.first().amount, AMOUNT_DELTA)
    }

    @Test
    fun filterByInclusiveStartDate() = runBlocking {
        val oct1 = 1_760_000_000_000L  // Oct 1, 2025
        val oct2 = 1_760_086_400_000L  // Oct 2, 2025

        insertExpense(amount = 10.0, categoryId = 1L, description = "Oct 1", date = oct1)
        insertExpense(amount = 20.0, categoryId = 1L, description = "Oct 2", date = oct2)

        val results = expenseDao.searchExpenses(
            query = null,
            categoryIds = emptyList(),
            categoryIdCount = 0,
            minAmountMinor = null,
            maxAmountMinor = null,
            startTime = oct1,
            endTime = null,
            sortOrder = "NEWEST"
        ).first()

        assertEquals(2, results.size)
    }

    @Test
    fun filterByExclusiveEndDate() = runBlocking {
        val oct1 = 1_760_000_000_000L  // Oct 1, 2025
        val oct2 = 1_760_086_400_000L  // Oct 2, 2025

        insertExpense(amount = 10.0, categoryId = 1L, description = "Oct 1", date = oct1)
        insertExpense(amount = 20.0, categoryId = 1L, description = "Oct 2", date = oct2)

        // End date exclusive: Oct 2 should NOT be included when endTime = oct2
        val results = expenseDao.searchExpenses(
            query = null,
            categoryIds = emptyList(),
            categoryIdCount = 0,
            minAmountMinor = null,
            maxAmountMinor = null,
            startTime = null,
            endTime = oct2,
            sortOrder = "NEWEST"
        ).first()

        assertEquals(1, results.size)
        assertEquals(oct1, results.first().date)
    }

    @Test
    fun sortByNewestFirst() = runBlocking {
        val date1 = 1_760_000_000_000L
        val date2 = 1_760_086_400_000L

        insertExpense(amount = 10.0, categoryId = 1L, description = "Older", date = date1)
        insertExpense(amount = 20.0, categoryId = 1L, description = "Newer", date = date2)

        val results = expenseDao.searchExpenses(
            query = null,
            categoryIds = emptyList(),
            categoryIdCount = 0,
            minAmountMinor = null,
            maxAmountMinor = null,
            startTime = null,
            endTime = null,
            sortOrder = "NEWEST"
        ).first()

        assertEquals(2, results.size)
        assertEquals(date2, results.first().date)
        assertEquals(date1, results.get(1).date)
    }

    @Test
    fun sortByOldestFirst() = runBlocking {
        val date1 = 1_760_000_000_000L
        val date2 = 1_760_086_400_000L

        insertExpense(amount = 10.0, categoryId = 1L, description = "Older", date = date1)
        insertExpense(amount = 20.0, categoryId = 1L, description = "Newer", date = date2)

        val results = expenseDao.searchExpenses(
            query = null,
            categoryIds = emptyList(),
            categoryIdCount = 0,
            minAmountMinor = null,
            maxAmountMinor = null,
            startTime = null,
            endTime = null,
            sortOrder = "OLDEST"
        ).first()

        assertEquals(2, results.size)
        assertEquals(date1, results.first().date)
        assertEquals(date2, results.get(1).date)
    }

    @Test
    fun sortByHighestAmountFirst() = runBlocking {
        insertExpense(amount = 10.0, categoryId = 1L, description = "Low", date = 1_760_000_000_000L)
        insertExpense(amount = 100.0, categoryId = 1L, description = "High", date = 1_760_000_000_000L)
        insertExpense(amount = 50.0, categoryId = 1L, description = "Medium", date = 1_760_000_000_000L)

        val results = expenseDao.searchExpenses(
            query = null,
            categoryIds = emptyList(),
            categoryIdCount = 0,
            minAmountMinor = null,
            maxAmountMinor = null,
            startTime = null,
            endTime = null,
            sortOrder = "HIGHEST_AMOUNT"
        ).first()

        assertEquals(3, results.size)
        assertEquals(100.0, results.first().amount, AMOUNT_DELTA)
        assertEquals(50.0, results.get(1).amount, AMOUNT_DELTA)
        assertEquals(10.0, results.get(2).amount, AMOUNT_DELTA)
    }

    @Test
    fun sortByLowestAmountFirst() = runBlocking {
        insertExpense(amount = 10.0, categoryId = 1L, description = "Low", date = 1_760_000_000_000L)
        insertExpense(amount = 100.0, categoryId = 1L, description = "High", date = 1_760_000_000_000L)
        insertExpense(amount = 50.0, categoryId = 1L, description = "Medium", date = 1_760_000_000_000L)

        val results = expenseDao.searchExpenses(
            query = null,
            categoryIds = emptyList(),
            categoryIdCount = 0,
            minAmountMinor = null,
            maxAmountMinor = null,
            startTime = null,
            endTime = null,
            sortOrder = "LOWEST_AMOUNT"
        ).first()

        assertEquals(3, results.size)
        assertEquals(10.0, results.first().amount, AMOUNT_DELTA)
        assertEquals(50.0, results.get(1).amount, AMOUNT_DELTA)
        assertEquals(100.0, results.get(2).amount, AMOUNT_DELTA)
    }

    @Test
    fun combinedFiltersWorkTogether() = runBlocking {
        insertExpense(amount = 10.0, categoryId = 1L, description = "Coffee", date = 1_760_000_000_000L)
        insertExpense(amount = 50.0, categoryId = 1L, description = "Coffee", date = 1_760_086_400_000L)
        insertExpense(amount = 100.0, categoryId = 2L, description = "Coffee", date = 1_760_172_800_000L)
        insertExpense(amount = 20.0, categoryId = 1L, description = "Tea", date = 1_760_000_000_000L)

        val results = expenseDao.searchExpenses(
            query = "coffee",
            categoryIds = listOf(1L),
            categoryIdCount = 1,
            minAmountMinor = 2000L,
            maxAmountMinor = null,
            startTime = null,
            endTime = null,
            sortOrder = "HIGHEST_AMOUNT"
        ).first()

        assertEquals(1, results.size)
        assertEquals(50.0, results.first().amount, AMOUNT_DELTA)
        assertEquals(1L, results.first().categoryId)
        assertTrue(results.first().description.lowercase().contains("coffee"))
    }

    @Test
    fun noMatchingExpensesReturnsEmptyList() = runBlocking {
        insertExpense(amount = 10.0, categoryId = 1L, description = "Foo", date = 1_760_000_000_000L)

        val results = expenseDao.searchExpenses(
            query = "Bar",
            categoryIds = emptyList(),
            categoryIdCount = 0,
            minAmountMinor = null,
            maxAmountMinor = null,
            startTime = null,
            endTime = null,
            sortOrder = "NEWEST"
        ).first()

        assertTrue(results.isEmpty())
    }

    @Test
    fun deterministicOrderingForEqualValues() = runBlocking {
        // Same date and amount - should be ordered by id ASC
        insertExpense(id = 1L, amount = 50.0, categoryId = 1L, description = "A", date = 1_760_000_000_000L)
        insertExpense(id = 2L, amount = 50.0, categoryId = 1L, description = "B", date = 1_760_000_000_000L)
        insertExpense(id = 3L, amount = 50.0, categoryId = 1L, description = "C", date = 1_760_000_000_000L)

        val results = expenseDao.searchExpenses(
            query = null,
            categoryIds = emptyList(),
            categoryIdCount = 0,
            minAmountMinor = null,
            maxAmountMinor = null,
            startTime = null,
            endTime = null,
            sortOrder = "NEWEST"
        ).first()

        assertEquals(3, results.size)
        assertEquals(1L, results.first().id)
        assertEquals(2L, results.get(1).id)
        assertEquals(3L, results.get(2).id)
    }
}
