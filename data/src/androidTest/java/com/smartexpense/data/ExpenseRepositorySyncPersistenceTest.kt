package com.smartexpense.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartexpense.database.AppDatabase
import com.smartexpense.database.entity.ExpenseEntity
import com.smartexpense.database.entity.PendingExpenseDeletionEntity
import com.smartexpense.database.entity.SyncStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExpenseRepositorySyncPersistenceTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase

    private val databaseName = "expense_sync_persistence_test.db"

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
    fun pendingExpenseRemainsPendingAfterDatabaseReopens() = runTest {
        database.expenseDao().insertExpense(
            ExpenseEntity(
                id = 1L,
                amountMinor = 20000,
                categoryId = 1L,
                description = "Pending upload",
                date = 1_760_000_000_000L,
                syncStatus = SyncStatus.PENDING,
                clientId = "pending-upload-1"
            )
        )

        database.close()
        database = openDatabase()

        val pendingExpenses = database.expenseDao().getUnsyncedExpenses()

        assertEquals(1, pendingExpenses.size)
        assertEquals(
            SyncStatus.PENDING,
            pendingExpenses.first().syncStatus
        )
        assertEquals("pending-upload-1", pendingExpenses.first().clientId)
    }

    @Test
    fun pendingDeletionRemainsQueuedAfterDatabaseReopens() = runTest {
        database.pendingExpenseDeletionDao().insert(
            PendingExpenseDeletionEntity(
                clientId = "pending-delete-1",
                remoteId = 42L
            )
        )

        database.close()
        database = openDatabase()

        val pendingDeletions =
            database.pendingExpenseDeletionDao().getPendingDeletions()

        assertEquals(1, pendingDeletions.size)
        assertEquals("pending-delete-1", pendingDeletions.first().clientId)
        assertEquals(42L, pendingDeletions.first().remoteId)
    }

    @Test
    fun clearingCompletedDeletionDoesNotRemoveOtherPendingDeletions() = runTest {
        database.pendingExpenseDeletionDao().insert(
            PendingExpenseDeletionEntity(clientId = "completed-delete")
        )
        database.pendingExpenseDeletionDao().insert(
            PendingExpenseDeletionEntity(clientId = "still-pending-delete")
        )

        database.pendingExpenseDeletionDao()
            .deleteByClientId("completed-delete")

        val remainingDeletions =
            database.pendingExpenseDeletionDao().getPendingDeletions()

        assertEquals(1, remainingDeletions.size)
        assertEquals(
            "still-pending-delete",
            remainingDeletions.first().clientId
        )
        assertTrue(
            remainingDeletions.none {
                it.clientId == "completed-delete"
            }
        )
    }

    @Test
    fun concurrentExpenseInsertsPreserveEveryExpense() = runTest {
        val expenses = (1L..20L).map { id ->
            ExpenseEntity(
                id = id,
                amountMinor = id * 1000,
                categoryId = 1L,
                description = "Expense $id",
                date = 1_760_000_000_000L + id,
                syncStatus = SyncStatus.PENDING,
                clientId = "concurrent-expense-$id"
            )
        }

        expenses.map { expense ->
            async {
                database.expenseDao().insertExpense(expense)
            }
        }.awaitAll()

        val storedExpenses = database.expenseDao()
            .getExpenses()
            .first()

        assertEquals(20, storedExpenses.size)
        assertEquals(
            20,
            storedExpenses.map { it.clientId }.distinct().size
        )
    }

    @Test
    fun concurrentPendingDeletionInsertsPreserveEveryDeletion() = runTest {
        val deletions = (1L..20L).map { id ->
            PendingExpenseDeletionEntity(
                clientId = "concurrent-deletion-$id",
                remoteId = id
            )
        }

        deletions.map { deletion ->
            async {
                database.pendingExpenseDeletionDao().insert(deletion)
            }
        }.awaitAll()

        val storedDeletions =
            database.pendingExpenseDeletionDao().getPendingDeletions()

        assertEquals(20, storedDeletions.size)
        assertEquals(
            20,
            storedDeletions.map { it.clientId }.distinct().size
        )
    }

    @Test
    fun concurrentDeletionCleanupPreservesUnrelatedPendingDeletions() = runTest {
        val deletions = (1L..10L).map { id ->
            PendingExpenseDeletionEntity(
                clientId = "deletion-$id",
                remoteId = id
            )
        }

        deletions.forEach {
            database.pendingExpenseDeletionDao().insert(it)
        }

        (1L..5L).map { id ->
            async {
                database.pendingExpenseDeletionDao()
                    .deleteByClientId("deletion-$id")
            }
        }.awaitAll()

        val remaining =
            database.pendingExpenseDeletionDao().getPendingDeletions()

        assertEquals(5, remaining.size)
        assertTrue(
            remaining.all {
                it.clientId in (6L..10L).map { id -> "deletion-$id" }
            }
        )
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