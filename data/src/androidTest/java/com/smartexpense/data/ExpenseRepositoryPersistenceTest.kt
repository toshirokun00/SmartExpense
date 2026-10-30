package com.smartexpense.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartexpense.database.AppDatabase
import com.smartexpense.database.entity.ExpenseEntity
import com.smartexpense.database.entity.PendingExpenseDeletionEntity
import com.smartexpense.database.entity.SyncStatus
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExpenseRepositoryPersistenceTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase

    private val databaseName = "expense_persistence_test.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        context.deleteDatabase(databaseName)
        database = openDatabase()
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun expensePersistsAfterDatabaseReopens() = runTest {
        val expense = ExpenseEntity(
            id = 1L,
            amountMinor = 25000,
            categoryId = 1L,
            description = "Persistent expense",
            date = 1_760_000_000_000L,
            syncStatus = SyncStatus.PENDING,
            clientId = "persistent-expense-1"
        )

        database.expenseDao().insertExpense(expense)

        database.close()
        database = openDatabase()

        val persistedExpense =
            database.expenseDao().getExpenseById(1L)

        assertNotNull(persistedExpense)
        assertEquals(25000L, persistedExpense!!.amountMinor)
        assertEquals(
            "Persistent expense",
            persistedExpense.description
        )
        assertEquals(
            SyncStatus.PENDING,
            persistedExpense.syncStatus
        )
        assertEquals(
            "persistent-expense-1",
            persistedExpense.clientId
        )
    }

    @Test
    fun pendingDeletionPersistsAfterDatabaseReopens() = runTest {
        val deletion = PendingExpenseDeletionEntity(
            clientId = "pending-deletion-1",
            remoteId = 123L
        )

        database.pendingExpenseDeletionDao().insert(deletion)

        database.close()
        database = openDatabase()

        val pendingDeletions =
            database.pendingExpenseDeletionDao()
                .getPendingDeletions()

        assertEquals(1, pendingDeletions.size)
        assertEquals(
            "pending-deletion-1",
            pendingDeletions.first().clientId
        )
        assertEquals(
            123L,
            pendingDeletions.first().remoteId
        )
    }

    @Test
    fun deletedExpenseRemainsAbsentAfterDatabaseReopens() = runTest {
        val expense = ExpenseEntity(
            id = 1L,
            amountMinor = 10000,
            categoryId = 1L,
            description = "Expense to delete",
            date = 1_760_000_000_000L,
            syncStatus = SyncStatus.PENDING,
            clientId = "deleted-expense-1"
        )

        database.expenseDao().insertExpense(expense)
        database.expenseDao().deleteExpenseById(1L)

        database.close()
        database = openDatabase()

        val persistedExpense =
            database.expenseDao().getExpenseById(1L)

        assertNull(persistedExpense)
    }

    private fun openDatabase(): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            databaseName
        )
            .allowMainThreadQueries()
            .build()
}