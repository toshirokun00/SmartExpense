package com.smartexpense.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartexpense.database.AppDatabase
import com.smartexpense.database.ExpenseDeletionTransaction
import com.smartexpense.database.entity.ExpenseEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExpenseDeletionTransactionTest {

    private lateinit var database: AppDatabase
    private lateinit var transaction: ExpenseDeletionTransaction

    private val expenseId = 1L
    private val clientId = "test-client-id"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        transaction = ExpenseDeletionTransaction(database)
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    @Test
    fun deleteExpenseRemovesExpenseAndRecordsPendingDeletion() =
        runBlocking {
            // Arrange
            database.expenseDao().insertExpense(
                ExpenseEntity(
                    id = expenseId,
                    amountMinor = 15000,
                    categoryId = 1L,
                    description = "Test expense",
                    date = 1_760_000_000_000L,
                    clientId = clientId
                )
            )

            // Act
            transaction.deleteExpense(
                expenseId = expenseId,
                clientId = clientId
            )

            // Assert
            val deletedExpense =
                database.expenseDao().getExpenseById(expenseId)

            val pendingDeletions =
                database.pendingExpenseDeletionDao()
                    .getPendingDeletions()

            assertNull(deletedExpense)
            assertEquals(1, pendingDeletions.size)
            assertEquals(clientId, pendingDeletions.first().clientId)
        }

    @Test
    fun deleteExpenseRollsBackWhenExpenseDeletionFails() =
        runBlocking {
            // Arrange
            database.expenseDao().insertExpense(
                ExpenseEntity(
                    id = expenseId,
                    amountMinor = 15000,
                    categoryId = 1L,
                    description = "Test expense",
                    date = 1_760_000_000_000L,
                    clientId = clientId
                )
            )

            // Force the expense DELETE statement to fail.
            // The pending deletion INSERT happens first inside the
            // same database transaction.
            database.openHelper.writableDatabase.execSQL(
                """
                CREATE TRIGGER fail_expense_delete
                BEFORE DELETE ON expenses
                BEGIN
                    SELECT RAISE(ABORT, 'forced delete failure');
                END
                """.trimIndent()
            )

            // Act
            var thrown = false

            try {
                transaction.deleteExpense(
                    expenseId = expenseId,
                    clientId = clientId
                )
            } catch (exception: Exception) {
                thrown = true
            }

            // Assert: the transaction must roll back both operations.
            assertTrue(
                "Expected expense deletion to fail",
                thrown
            )

            val existingExpense =
                database.expenseDao().getExpenseById(expenseId)

            val pendingDeletions =
                database.pendingExpenseDeletionDao()
                    .getPendingDeletions()

            assertNotNull(existingExpense)
            assertEquals(clientId, existingExpense?.clientId)
            assertTrue(pendingDeletions.isEmpty())
        }
}