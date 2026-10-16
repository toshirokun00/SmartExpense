package com.smartexpense.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartexpense.database.AppDatabase
import com.smartexpense.database.entity.ExpenseEntity
import com.smartexpense.database.entity.SyncStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExpenseRepositoryOfflineFirstTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun expenseIsAvailableLocallyWhenRemoteSyncHasNotOccurred() = runTest {
        val expense = ExpenseEntity(
            amount = 250.0,
            categoryId = 1L,
            description = "Offline expense",
            date = 1_760_000_000_000L,
            syncStatus = SyncStatus.PENDING,
            clientId = "offline-expense-1"
        )

        database.expenseDao().insertExpense(expense)

        val localExpenses = database.expenseDao()
            .getExpenses()
            .first()

        assertEquals(1, localExpenses.size)
        assertEquals("Offline expense", localExpenses.first().description)
        assertEquals(250.0, localExpenses.first().amount, 0.001)
        assertEquals(
            SyncStatus.PENDING,
            localExpenses.first().syncStatus
        )
    }

    @Test
    fun pendingExpenseRemainsAvailableForSync() = runTest {
        val expense = ExpenseEntity(
            amount = 125.50,
            categoryId = 1L,
            description = "Pending sync",
            date = 1_760_000_000_000L,
            syncStatus = SyncStatus.PENDING,
            clientId = "pending-expense-1"
        )

        database.expenseDao().insertExpense(expense)

        val unsyncedExpenses = database.expenseDao()
            .getUnsyncedExpenses()

        assertEquals(1, unsyncedExpenses.size)
        assertEquals(
            "pending-expense-1",
            unsyncedExpenses.first().clientId
        )
    }

    @Test
    fun syncedExpenseIsNotReturnedAsUnsynced() = runTest {
        val expense = ExpenseEntity(
            amount = 300.0,
            categoryId = 1L,
            description = "Synced expense",
            date = 1_760_000_000_000L,
            syncStatus = SyncStatus.PENDING,
            clientId = "synced-expense-1"
        )

        database.expenseDao().insertExpense(expense)

        database.expenseDao().updateSyncStatus(
            expenseId = 1L,
            status = SyncStatus.SYNCED.name
        )

        val unsyncedExpenses = database.expenseDao()
            .getUnsyncedExpenses()

        assertTrue(unsyncedExpenses.isEmpty())
    }

}
