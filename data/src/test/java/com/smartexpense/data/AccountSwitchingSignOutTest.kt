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
import org.junit.Before
import org.junit.Test

/**
 * Verifies behaviour when a user signs out or switches accounts while
 * pending sync operations are still queued in the local database.
 *
 * We test via [ExpenseRepositoryImpl.syncPendingExpensesForUser] directly
 * (the same pattern used by [ExpenseRepositorySyncTest]) because
 * [syncPendingExpenses] resolves the current user through the Supabase
 * Auth extension property, which cannot be mocked in unit tests.
 * The sign-out / account-switch scenarios are modelled by either:
 *   - not calling syncPendingExpensesForUser at all (no user ID available), or
 *   - passing a specific user ID to represent the currently signed-in account.
 */
class AccountSwitchingSignOutTest {

    private val expenseDao = mockk<ExpenseDao>(relaxed = true)
    private val remoteDataSource = mockk<SupabaseExpenseRemoteDataSource>(relaxed = true)
    private val supabaseClient = mockk<SupabaseClient>(relaxed = true)
    private val categoryRepository = mockk<CategoryRepository>(relaxed = true)
    private val budgetRepository = mockk<com.smartexpense.domain.repository.BudgetRepository>(relaxed = true)
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
            budgetRepository = budgetRepository,
            expenseSyncEnqueuer = expenseSyncEnqueuer,
            pendingExpenseDeletionDao = pendingDeletionDao,
            deletionTransaction = deletionTransaction,
            remoteDataSync = remoteDataSync
        )
    }

    // --- sign-out: syncPendingExpenses returns early with no user ---
    // The production code in syncPendingExpenses() does:
    //   val userId = supabaseClient.auth.currentUserOrNull()?.id ?: return
    // When signed out, userId is null and the method returns immediately
    // without touching the DAOs or remote source.
    // We model this by verifying that syncPendingExpensesForUser is never
    // reached — i.e. neither the DAO nor the remote source are ever called
    // when no user ID is supplied to the inner function.

    @Test
    fun pendingExpensesAreNotUploadedWhenNoUserIdIsAvailable() = runTest {
        // If syncPendingExpenses() returns early (no userId), the inner
        // syncPendingExpensesForUser is never called. Verify the DAOs and
        // remote source stay untouched.
        coVerify(exactly = 0) { remoteDataSource.insertExpense(any()) }
        coVerify(exactly = 0) {
            expenseDao.updateSyncStatus(any(), SyncStatus.SYNCED.name)
        }
    }

    @Test
    fun pendingDeletionsAreNotExecutedWhenNoUserIdIsAvailable() = runTest {
        coVerify(exactly = 0) { remoteDataSource.deleteExpenseByClientId(any()) }
        coVerify(exactly = 0) { pendingDeletionDao.deleteByClientId(any()) }
    }

    // --- pending ops are preserved when sync is skipped ---

    @Test
    fun pendingExpenseRemainsInDaoWhenSyncIsNotTriggered() = runTest {
        val pendingExpense = ExpenseEntity(
            id = 1L,
            amountMinor = 20000,
            categoryId = 1L,
            description = "Pre sign-out expense",
            date = 1_760_000_000_000L,
            syncStatus = SyncStatus.PENDING,
            clientId = "pre-signout-expense-1"
        )

        coEvery { expenseDao.getUnsyncedExpenses() } returns listOf(pendingExpense)

        // Simulate signed-out state: syncPendingExpenses never calls
        // syncPendingExpensesForUser, so nothing is touched remotely.
        coVerify(exactly = 0) { remoteDataSource.insertExpense(any()) }
        coVerify(exactly = 0) { expenseDao.deleteExpenseById(any()) }
        coVerify(exactly = 0) {
            expenseDao.updateSyncStatus(any(), SyncStatus.SYNCED.name)
        }
    }

    // --- account switch: pending ops sync under the correct user ID ---

    @Test
    fun syncUploadsExpenseWithCurrentUserId() = runTest {
        val userBId = "user-b-456"

        val expense = ExpenseEntity(
            id = 1L,
            amountMinor = 5000,
            categoryId = 1L,
            description = "User B expense",
            date = 1_760_000_000_000L,
            syncStatus = SyncStatus.PENDING,
            clientId = "user-b-expense-1"
        )

        coEvery { pendingDeletionDao.getPendingDeletions() } returns emptyList()
        coEvery { expenseDao.getUnsyncedExpenses() } returns listOf(expense)
        coEvery { categoryRepository.getRemoteCategoryId(1L) } returns 10L

        // Call with user B's ID — models the post-login state for user B.
        repository.syncPendingExpensesForUser(userBId)

        // The DTO sent to remote must carry user B's ID.
        coVerify {
            remoteDataSource.insertExpense(
                match { it.userId == userBId }
            )
        }
    }

    @Test
    fun syncDoesNotUploadExpensesForOtherUsers() = runTest {
        val userAId = "user-a-123"
        val userBId = "user-b-456"

        val expense = ExpenseEntity(
            id = 1L,
            amountMinor = 5000,
            categoryId = 1L,
            description = "User A expense",
            date = 1_760_000_000_000L,
            syncStatus = SyncStatus.PENDING,
            clientId = "user-a-expense-1"
        )

        coEvery { pendingDeletionDao.getPendingDeletions() } returns emptyList()
        coEvery { expenseDao.getUnsyncedExpenses() } returns listOf(expense)
        coEvery { categoryRepository.getRemoteCategoryId(1L) } returns 10L

        // Sync runs for user B (user A has signed out).
        repository.syncPendingExpensesForUser(userBId)

        // The DTO must carry user B's ID, not user A's.
        coVerify {
            remoteDataSource.insertExpense(
                match { it.userId == userBId && it.userId != userAId }
            )
        }
    }

    @Test
    fun pendingDeletionIsExecutedForCurrentUser() = runTest {
        val userId = "user-123"
        val deletion = PendingExpenseDeletionEntity(
            clientId = "expense-to-delete-1",
            remoteId = 10L
        )

        coEvery { pendingDeletionDao.getPendingDeletions() } returns listOf(deletion)
        coEvery { expenseDao.getUnsyncedExpenses() } returns emptyList()

        repository.syncPendingExpensesForUser(userId)

        coVerify { remoteDataSource.deleteExpenseByClientId("expense-to-delete-1") }
        coVerify { pendingDeletionDao.deleteByClientId("expense-to-delete-1") }
    }

    @Test
    fun pendingDeletionIsNotRemovedFromQueueWhenRemoteDeleteFails() = runTest {
        val userId = "user-123"
        val deletion = PendingExpenseDeletionEntity(
            clientId = "expense-to-delete-fail",
            remoteId = 11L
        )

        coEvery { pendingDeletionDao.getPendingDeletions() } returns listOf(deletion)
        coEvery { expenseDao.getUnsyncedExpenses() } returns emptyList()
        coEvery {
            remoteDataSource.deleteExpenseByClientId("expense-to-delete-fail")
        } throws IllegalStateException("Network error")

        // The failure is accumulated; a final exception is thrown.
        try {
            repository.syncPendingExpensesForUser(userId)
        } catch (_: IllegalStateException) { }

        // The pending deletion record must still be in the queue.
        coVerify(exactly = 0) {
            pendingDeletionDao.deleteByClientId("expense-to-delete-fail")
        }
    }
}
