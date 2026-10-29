package com.smartexpense.data.repositoryimpl

import com.smartexpense.data.mapper.toDomain
import com.smartexpense.data.mapper.toEntity
import com.smartexpense.data.remote.SupabaseBudgetDto
import com.smartexpense.data.remote.SupabaseBudgetRemoteDataSource
import com.smartexpense.data.sync.ExpenseSyncEnqueuer
import com.smartexpense.database.BudgetDeletionTransaction
import com.smartexpense.database.dao.BudgetDao
import com.smartexpense.database.dao.PendingBudgetDeletionDao
import com.smartexpense.database.entity.SyncStatus
import com.smartexpense.domain.model.Budget
import com.smartexpense.domain.repository.BudgetRepository
import com.smartexpense.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException

class BudgetRepositoryImpl(
    private val budgetDao: BudgetDao,
    private val supabaseBudgetRemoteDataSource: SupabaseBudgetRemoteDataSource,
    private val categoryRepository: CategoryRepository,
    private val expenseSyncEnqueuer: ExpenseSyncEnqueuer,
    private val pendingBudgetDeletionDao: PendingBudgetDeletionDao,
    private val deletionTransaction: BudgetDeletionTransaction
) : BudgetRepository {

    override fun getBudgets(): Flow<List<Budget>> =
        budgetDao.getBudgets().map { budgets -> budgets.map { it.toDomain() } }

    override suspend fun getBudgetById(id: Long): Budget? =
        budgetDao.getBudgetById(id)?.toDomain()

    override suspend fun addBudget(budget: Budget) {
        budgetDao.insertBudget(budget.toEntity().copy(syncStatus = SyncStatus.PENDING))
        expenseSyncEnqueuer.enqueue()
    }

    override suspend fun updateBudget(budget: Budget) {
        val existing = budgetDao.getBudgetById(budget.id) ?: return
        budgetDao.updateBudget(
            existing.copy(
                categoryId = budget.categoryId,
                amount = budget.amount,
                startDate = budget.startDate,
                endDate = budget.endDate,
                syncStatus = SyncStatus.PENDING
            )
        )
        expenseSyncEnqueuer.enqueue()
    }

    override suspend fun deleteBudget(budget: Budget) {
        val existing = budgetDao.getBudgetById(budget.id)
            ?: return

        deletionTransaction.deleteBudget(
            budgetId = existing.id,
            clientId = existing.clientId
        )

        expenseSyncEnqueuer.enqueue()
    }

    override suspend fun syncBudgets(userId: String) {
        var failed = false

        for (deletion in pendingBudgetDeletionDao.getPendingDeletions()) {
            try {
                supabaseBudgetRemoteDataSource.deleteBudgetByClientId(
                    deletion.clientId
                )
                pendingBudgetDeletionDao.deleteByClientId(deletion.clientId)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                failed = true
            }
        }

        for (budget in budgetDao.getUnsyncedBudgets()) {
            // Never upload one user's pending rows under another account.
            if (budget.userId != null && budget.userId != userId) {
                continue
            }

            try {
                val remoteCategoryId =
                    categoryRepository.getRemoteCategoryId(budget.categoryId)
                        ?: error("Could not resolve remote category ID for budget")

                supabaseBudgetRemoteDataSource.upsertBudget(
                    SupabaseBudgetDto(
                        clientId = budget.clientId,
                        userId = userId,
                        categoryId = remoteCategoryId,
                        amount = budget.amount,
                        startDate = Instant.ofEpochMilli(budget.startDate).toString(),
                        endDate = Instant.ofEpochMilli(budget.endDate).toString()
                    )
                )

                budgetDao.updateSyncStatus(
                    budgetId = budget.id,
                    status = SyncStatus.SYNCED.name
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                budgetDao.updateSyncStatus(
                    budgetId = budget.id,
                    status = SyncStatus.FAILED.name
                )
                failed = true
            }
        }

        if (failed) {
            error("Some budgets failed to synchronize")
        }
    }
}
