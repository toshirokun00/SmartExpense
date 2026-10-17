package com.smartexpense.data

import com.smartexpense.data.remote.SupabaseExpenseRemoteDataSource
import com.smartexpense.data.repositoryimpl.ExpenseRepositoryImpl
import com.smartexpense.data.sync.ExpenseSyncEnqueuer
import com.smartexpense.data.sync.RemoteDataSync
import com.smartexpense.database.ExpenseDeletionTransaction
import com.smartexpense.database.dao.ExpenseDao
import com.smartexpense.database.dao.PendingExpenseDeletionDao
import com.smartexpense.database.entity.ExpenseEntity
import com.smartexpense.database.entity.PendingExpenseDeletionEntity
import com.smartexpense.database.entity.SyncStatus
import com.smartexpense.domain.repository.CategoryRepository
import io.github.jan.supabase.SupabaseClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test

class ExpenseRepositorySyncTest {

    private val expenseDao = mockk<ExpenseDao>(relaxed = true)
    private val remoteDataSource =
        mockk<SupabaseExpenseRemoteDataSource>(relaxed = true)
    private val supabaseClient = mockk<SupabaseClient>(relaxed = true)
    private val categoryRepository =
        mockk<CategoryRepository>(relaxed = true)
    private val pendingDeletionDao =
        mockk<PendingExpenseDeletionDao>(relaxed = true)
    private val deletionTransaction =
        mockk<ExpenseDeletionTransaction>(relaxed = true)

    private val expenseSyncEnqueuer =
        mockk<ExpenseSyncEnqueuer>(relaxed = true)

    private val remoteDataSync =
        mockk<RemoteDataSync>(relaxed = true)

    @Test
    fun successfulRemoteDeletionRemovesPendingDeletion() = runTest {
        val deletion = PendingExpenseDeletionEntity(
            clientId = "test-client-id"
        )

        coEvery {
            pendingDeletionDao.getPendingDeletions()
        } returns listOf(deletion)

        coEvery {
            expenseDao.getUnsyncedExpenses()
        } returns emptyList()

        val repository = createRepository()

        repository.syncPendingExpensesForUser("test-user-id")

        coVerify {
            remoteDataSource.deleteExpenseByClientId("test-client-id")
        }

        coVerify {
            pendingDeletionDao.deleteByClientId("test-client-id")
        }
    }

    @Test
    fun failedRemoteDeletionKeepsPendingDeletionAndThrows() = runTest {
        val deletion = PendingExpenseDeletionEntity(
            clientId = "test-client-id"
        )

        coEvery {
            pendingDeletionDao.getPendingDeletions()
        } returns listOf(deletion)

        coEvery {
            expenseDao.getUnsyncedExpenses()
        } returns emptyList()

        coEvery {
            remoteDataSource.deleteExpenseByClientId("test-client-id")
        } throws IllegalStateException("Network error")

        val repository = createRepository()

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking {
                repository.syncPendingExpensesForUser("test-user-id")
            }
        }


        coVerify(exactly = 0) {
            pendingDeletionDao.deleteByClientId("test-client-id")
        }
    }

    @Test
    fun successfulExpenseSyncMarksExpenseAsSynced() = runTest {
        val expense = ExpenseEntity(
            id = 1L,
            amount = 150.0,
            categoryId = 1L,
            description = "Lunch",
            date = 1_760_000_000_000L,
            syncStatus = SyncStatus.PENDING,
            clientId = "test-expense-client-id"
        )

        coEvery {
            pendingDeletionDao.getPendingDeletions()
        } returns emptyList()

        coEvery {
            expenseDao.getUnsyncedExpenses()
        } returns listOf(expense)

        val repository = createRepository()

        repository.syncPendingExpensesForUser("test-user-id")

        coVerify {
            remoteDataSource.insertExpense(
                match { it.clientId == "test-expense-client-id" }
            )
        }

        coVerify {
            expenseDao.updateSyncStatus(
                expense.id,
                SyncStatus.SYNCED.name
            )
        }
    }

    @Test
    fun failedExpenseSyncCanBeRetriedSuccessfully() = runTest {
        val expense = ExpenseEntity(
            id = 1L,
            amount = 150.0,
            categoryId = 1L,
            description = "Lunch",
            date = 1_760_000_000_000L,
            syncStatus = SyncStatus.PENDING,
            clientId = "test-expense-client-id"
        )

        coEvery {
            pendingDeletionDao.getPendingDeletions()
        } returns emptyList()

        coEvery {
            expenseDao.getUnsyncedExpenses()
        } returns listOf(expense)

        coEvery {
            remoteDataSource.insertExpense(any())
        } throws IllegalStateException("Network error") andThen Unit

        val repository = createRepository()

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking {
                repository.syncPendingExpensesForUser("test-user-id")
            }
        }

        // Retry the same pending expense.
        repository.syncPendingExpensesForUser("test-user-id")

        coVerify(exactly = 2) {
            remoteDataSource.insertExpense(any())
        }

        coVerify {
            expenseDao.updateSyncStatus(
                expense.id,
                SyncStatus.SYNCED.name
            )
        }
    }

    private fun createRepository() = ExpenseRepositoryImpl(
        expenseDao = expenseDao,
        supabaseExpenseRemoteDataSource = remoteDataSource,
        supabaseClient = supabaseClient,
        categoryRepository = categoryRepository,
        expenseSyncEnqueuer = expenseSyncEnqueuer,
        pendingExpenseDeletionDao = pendingDeletionDao,
        deletionTransaction = deletionTransaction,
        remoteDataSync = remoteDataSync
    )

    @Test
    fun failedExpenseIsRetriedAndMarkedAsSynced() = runTest {
        val expense = ExpenseEntity(
            id = 1L,
            amount = 100.0,
            categoryId = 1L,
            description = "Retry expense",
            date = 1_760_000_000_000L,
            syncStatus = SyncStatus.FAILED,
            clientId = "retry-expense-1"
        )

        coEvery { expenseDao.getUnsyncedExpenses() } returns listOf(expense)
        coEvery {
            categoryRepository.getRemoteCategoryId(1L)
        } returns 10L

        val repository = createRepository()

        repository.syncPendingExpensesForUser("test-user-id")

        coVerify {
            remoteDataSource.insertExpense(
                match { it.clientId == "retry-expense-1" }
            )
        }

        coVerify {
            expenseDao.updateSyncStatus(
                expenseId = 1L,
                status = SyncStatus.SYNCED.name
            )
        }
    }

    @Test
    fun failedExpenseRemainsFailedWhenRetryFails() = runTest {
        val expense = ExpenseEntity(
            id = 1L,
            amount = 100.0,
            categoryId = 1L,
            description = "Retry expense",
            date = 1_760_000_000_000L,
            syncStatus = SyncStatus.FAILED,
            clientId = "retry-expense-1"
        )

        coEvery { expenseDao.getUnsyncedExpenses() } returns listOf(expense)
        coEvery {
            categoryRepository.getRemoteCategoryId(1L)
        } returns 10L
        coEvery {
            remoteDataSource.insertExpense(any())
        } throws IllegalStateException("Network error")

        val repository = createRepository()

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking {
                repository.syncPendingExpensesForUser("test-user-id")
            }
        }

        coVerify {
            expenseDao.updateSyncStatus(
                expenseId = 1L,
                status = SyncStatus.FAILED.name
            )
        }
    }
}
