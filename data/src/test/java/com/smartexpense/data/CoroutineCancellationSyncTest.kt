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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

/**
 * Verifies that coroutine cancellation is propagated correctly and that
 * database transactions are left in a consistent state when a sync
 * coroutine is cancelled mid-flight.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineCancellationSyncTest {

    private val expenseDao = mockk<ExpenseDao>(relaxed = true)
    private val remoteDataSource = mockk<SupabaseExpenseRemoteDataSource>(relaxed = true)
    private val supabaseClient = mockk<SupabaseClient>(relaxed = true)
    private val categoryRepository = mockk<CategoryRepository>(relaxed = true)
    private val pendingDeletionDao = mockk<PendingExpenseDeletionDao>(relaxed = true)
    private val deletionTransaction = mockk<ExpenseDeletionTransaction>(relaxed = true)
    private val expenseSyncEnqueuer = mockk<ExpenseSyncEnqueuer>(relaxed = true)
    private val remoteDataSync = mockk<RemoteDataSync>(relaxed = true)

    private lateinit var repository: ExpenseRepositoryImpl

    @Before
    fun setUp() {
        repository = ExpenseRepositoryImpl(
            expenseDao = expenseDao,
            supabaseExpenseRemoteDataSource = remoteDataSource,
            supabaseClient = supabaseClient,
            categoryRepository = categoryRepository,
            expenseSyncEnqueuer = expenseSyncEnqueuer,
            pendingExpenseDeletionDao = pendingDeletionDao,
            deletionTransaction = deletionTransaction,
            remoteDataSync = remoteDataSync
        )
    }

    // --- CancellationException propagation ---

    @Test
    fun cancellationExceptionFromRemoteInsertIsPropagated() = runTest {
        val expense = pendingExpense("cancel-expense-1")

        coEvery { pendingDeletionDao.getPendingDeletions() } returns emptyList()
        coEvery { expenseDao.getUnsyncedExpenses() } returns listOf(expense)
        coEvery { categoryRepository.getRemoteCategoryId(1L) } returns 10L
        coEvery { remoteDataSource.insertExpense(any()) } throws CancellationException()

        assertThrows(CancellationException::class.java) {
            kotlinx.coroutines.runBlocking {
                repository.syncPendingExpensesForUser("user-123")
            }
        }
    }

    @Test
    fun cancellationExceptionFromRemoteDeleteIsPropagated() = runTest {
        val deletion = PendingExpenseDeletionEntity(clientId = "cancel-deletion-1")

        coEvery { pendingDeletionDao.getPendingDeletions() } returns listOf(deletion)
        coEvery { expenseDao.getUnsyncedExpenses() } returns emptyList()
        coEvery {
            remoteDataSource.deleteExpenseByClientId(any())
        } throws CancellationException()

        assertThrows(CancellationException::class.java) {
            kotlinx.coroutines.runBlocking {
                repository.syncPendingExpensesForUser("user-123")
            }
        }
    }

    @Test
    fun cancellationExceptionFromRemoteDataSyncIsPropagated() = runTest {
        coEvery {
            remoteDataSync.pullRemoteData(any())
        } throws CancellationException()

        assertThrows(CancellationException::class.java) {
            kotlinx.coroutines.runBlocking {
                repository.syncPendingExpensesForUser("user-123")
            }
        }
    }

    // --- DB consistency when a non-cancellation error occurs ---

    @Test
    fun expenseStatusIsSetToFailedWhenSyncThrowsNonCancellationException() = runTest {
        val expense = pendingExpense("fail-expense-1")

        coEvery { pendingDeletionDao.getPendingDeletions() } returns emptyList()
        coEvery { expenseDao.getUnsyncedExpenses() } returns listOf(expense)
        coEvery { categoryRepository.getRemoteCategoryId(1L) } returns 10L
        coEvery { remoteDataSource.insertExpense(any()) } throws IllegalStateException("Network error")

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking {
                repository.syncPendingExpensesForUser("user-123")
            }
        }

        // Status must be FAILED — not left as PENDING, not marked SYNCED.
        coVerify {
            expenseDao.updateSyncStatus(
                expenseId = expense.id,
                status = SyncStatus.FAILED.name
            )
        }
        coVerify(exactly = 0) {
            expenseDao.updateSyncStatus(
                expenseId = expense.id,
                status = SyncStatus.SYNCED.name
            )
        }
    }

    @Test
    fun pendingDeletionIsNotRemovedWhenRemoteDeleteThrowsNonCancellationException() = runTest {
        val deletion = PendingExpenseDeletionEntity(clientId = "fail-deletion-1")

        coEvery { pendingDeletionDao.getPendingDeletions() } returns listOf(deletion)
        coEvery { expenseDao.getUnsyncedExpenses() } returns emptyList()
        coEvery {
            remoteDataSource.deleteExpenseByClientId("fail-deletion-1")
        } throws IllegalStateException("Network error")

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking {
                repository.syncPendingExpensesForUser("user-123")
            }
        }

        // The pending deletion record must still be in the queue.
        coVerify(exactly = 0) {
            pendingDeletionDao.deleteByClientId("fail-deletion-1")
        }
    }

    @Test
    fun secondExpenseIsStillAttemptedAfterFirstExpenseFails() = runTest {
        val failingExpense = pendingExpense(clientId = "fail-expense-1", id = 1L)
        val successExpense = pendingExpense(clientId = "ok-expense-2", id = 2L)

        coEvery { pendingDeletionDao.getPendingDeletions() } returns emptyList()
        coEvery { expenseDao.getUnsyncedExpenses() } returns listOf(failingExpense, successExpense)
        coEvery { categoryRepository.getRemoteCategoryId(1L) } returns 10L

        coEvery {
            remoteDataSource.insertExpense(match { it.clientId == "fail-expense-1" })
        } throws IllegalStateException("Network error")

        coEvery {
            remoteDataSource.insertExpense(match { it.clientId == "ok-expense-2" })
        } returns Unit

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking {
                repository.syncPendingExpensesForUser("user-123")
            }
        }

        // Both expenses must have been attempted.
        coVerify {
            remoteDataSource.insertExpense(match { it.clientId == "fail-expense-1" })
        }
        coVerify {
            remoteDataSource.insertExpense(match { it.clientId == "ok-expense-2" })
        }

        // Failing one → FAILED, successful one → SYNCED.
        coVerify {
            expenseDao.updateSyncStatus(failingExpense.id, SyncStatus.FAILED.name)
        }
        coVerify {
            expenseDao.updateSyncStatus(successExpense.id, SyncStatus.SYNCED.name)
        }
    }

    @Test
    fun syncJobCanBeCancelledFromOutside() = runTest(UnconfinedTestDispatcher()) {
        coEvery { remoteDataSync.pullRemoteData(any()) } coAnswers {
            // Simulate slow network by suspending long enough to be cancelled.
            kotlinx.coroutines.delay(10_000)
        }

        var caughtCancellation = false
        val job = launch {
            try {
                repository.syncPendingExpensesForUser("user-123")
            } catch (e: CancellationException) {
                caughtCancellation = true
                throw e
            }
        }

        job.cancel()
        job.join()

        // Cancellation must have been observed inside the coroutine.
        assertEquals(true, caughtCancellation)
    }

    // --- helpers ---

    private fun pendingExpense(
        clientId: String,
        id: Long = 1L
    ) = ExpenseEntity(
        id = id,
        amount = 100.0,
        categoryId = 1L,
        description = "Test",
        date = 1_760_000_000_000L,
        syncStatus = SyncStatus.PENDING,
        clientId = clientId
    )
}
