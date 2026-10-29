package com.smartexpense.data

import com.smartexpense.data.remote.SupabaseBudgetRemoteDataSource
import com.smartexpense.data.repositoryimpl.BudgetRepositoryImpl
import com.smartexpense.data.sync.ExpenseSyncEnqueuer
import com.smartexpense.database.BudgetDeletionTransaction
import com.smartexpense.database.dao.BudgetDao
import com.smartexpense.database.dao.PendingBudgetDeletionDao
import com.smartexpense.database.entity.BudgetEntity
import com.smartexpense.database.entity.SyncStatus
import com.smartexpense.domain.model.Budget
import com.smartexpense.domain.repository.CategoryRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class BudgetRepositoryImplTest {

    private lateinit var budgetDao: BudgetDao
    private lateinit var remoteDataSource: SupabaseBudgetRemoteDataSource
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var syncEnqueuer: ExpenseSyncEnqueuer
    private lateinit var pendingDeletionDao: PendingBudgetDeletionDao
    private lateinit var deletionTransaction: BudgetDeletionTransaction

    private lateinit var budgetRepository: BudgetRepositoryImpl

    @Before
    fun setup() {
        budgetDao = mockk(relaxed = true)
        remoteDataSource = mockk(relaxed = true)
        categoryRepository = mockk(relaxed = true)
        syncEnqueuer = mockk(relaxed = true)
        pendingDeletionDao = mockk(relaxed = true)
        deletionTransaction = mockk(relaxed = true)

        budgetRepository = BudgetRepositoryImpl(
            budgetDao = budgetDao,
            supabaseBudgetRemoteDataSource = remoteDataSource,
            categoryRepository = categoryRepository,
            expenseSyncEnqueuer = syncEnqueuer,
            pendingBudgetDeletionDao = pendingDeletionDao,
            deletionTransaction = deletionTransaction
        )
    }

    @Test
    fun addBudgetCallsDao() = runTest {
        val budget = Budget(
            id = 1L,
            categoryId = 1L,
            amount = 5000.0,
            startDate = 1L,
            endDate = 30L
        )

        coEvery {
            budgetDao.insertBudget(any())
        } just Runs

        budgetRepository.addBudget(budget)

        coVerify {
            budgetDao.insertBudget(
                match {
                    it.categoryId == 1L &&
                            it.amount == 5000.0 &&
                            it.startDate == 1L &&
                            it.endDate == 30L &&
                            it.syncStatus == SyncStatus.PENDING
                }
            )
        }
        coVerify { syncEnqueuer.enqueue() }
    }

    @Test
    fun addBudgetKeepsGeneratedClientId() = runTest {
        val inserted = slot<BudgetEntity>()
        coEvery { budgetDao.insertBudget(capture(inserted)) } just Runs

        budgetRepository.addBudget(
            Budget(categoryId = 1L, amount = 10.0, startDate = 1L, endDate = 2L)
        )

        assertEquals(true, inserted.captured.clientId.isNotBlank())
    }

    @Test
    fun deleteBudgetQueuesRemoteDeletion() = runTest {
        val entity = BudgetEntity(
            id = 7L,
            categoryId = 1L,
            amount = 10.0,
            startDate = 1L,
            endDate = 2L,
            clientId = "budget-7"
        )
        coEvery { budgetDao.getBudgetById(7L) } returns entity

        budgetRepository.deleteBudget(entity.toDomain())

        coVerify { deletionTransaction.deleteBudget(7L, "budget-7") }
        coVerify { syncEnqueuer.enqueue() }
    }

    @Test
    fun syncBudgetsUploadsPendingBudgetAndMarksItSynced() = runTest {
        val entity = BudgetEntity(
            id = 3L,
            categoryId = 5L,
            amount = 250.0,
            startDate = 100L,
            endDate = 200L,
            clientId = "budget-3",
            syncStatus = SyncStatus.PENDING
        )
        coEvery { pendingDeletionDao.getPendingDeletions() } returns emptyList()
        coEvery { budgetDao.getUnsyncedBudgets() } returns listOf(entity)
        coEvery { categoryRepository.getRemoteCategoryId(5L) } returns 55L

        budgetRepository.syncBudgets("user-1")

        coVerify {
            remoteDataSource.upsertBudget(
                match {
                    it.clientId == "budget-3" &&
                            it.userId == "user-1" &&
                            it.categoryId == 55L &&
                            it.amount == 250.0 &&
                            it.startDate == java.time.Instant.ofEpochMilli(100L).toString() &&
                            it.endDate == java.time.Instant.ofEpochMilli(200L).toString()
                }
            )
        }
        coVerify {
            budgetDao.updateSyncStatus(3L, SyncStatus.SYNCED.name)
        }
    }

    @Test
    fun syncBudgetsMarksBudgetFailedWhenUploadFails() = runTest {
        val entity = BudgetEntity(
            id = 4L,
            categoryId = 5L,
            amount = 10.0,
            startDate = 1L,
            endDate = 2L,
            clientId = "budget-4"
        )
        coEvery { pendingDeletionDao.getPendingDeletions() } returns emptyList()
        coEvery { budgetDao.getUnsyncedBudgets() } returns listOf(entity)
        coEvery { categoryRepository.getRemoteCategoryId(5L) } returns 55L
        coEvery { remoteDataSource.upsertBudget(any()) } throws
            IllegalStateException("offline")

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking {
                budgetRepository.syncBudgets("user-1")
            }
        }

        coVerify {
            budgetDao.updateSyncStatus(4L, SyncStatus.FAILED.name)
        }
    }

    @Test
    fun syncBudgetsProcessesQueuedDeletion() = runTest {
        coEvery {
            pendingDeletionDao.getPendingDeletions()
        } returns listOf(
            com.smartexpense.database.entity.PendingBudgetDeletionEntity("budget-9")
        )
        coEvery { budgetDao.getUnsyncedBudgets() } returns emptyList()

        budgetRepository.syncBudgets("user-1")

        coVerify { remoteDataSource.deleteBudgetByClientId("budget-9") }
        coVerify { pendingDeletionDao.deleteByClientId("budget-9") }
    }

    @Test
    fun syncBudgetsSkipsBudgetsOwnedByAnotherUser() = runTest {
        val entity = BudgetEntity(
            id = 5L,
            categoryId = 1L,
            amount = 10.0,
            startDate = 1L,
            endDate = 2L,
            clientId = "budget-5",
            userId = "other-user"
        )
        coEvery { pendingDeletionDao.getPendingDeletions() } returns emptyList()
        coEvery { budgetDao.getUnsyncedBudgets() } returns listOf(entity)

        budgetRepository.syncBudgets("user-1")

        coVerify(exactly = 0) { remoteDataSource.upsertBudget(any()) }
    }

    private fun BudgetEntity.toDomain() = Budget(
        id = id,
        categoryId = categoryId,
        amount = amount,
        startDate = startDate,
        endDate = endDate
    )
}
