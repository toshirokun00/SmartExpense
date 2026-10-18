package com.smartexpense.data.repositoryimpl

import com.smartexpense.data.remote.SupabaseExpenseDto
import com.smartexpense.data.remote.SupabaseExpenseRemoteDataSource
import com.smartexpense.data.repositoryimpl.mapper.toDomain
import com.smartexpense.data.repositoryimpl.mapper.toEntity
import com.smartexpense.data.sync.ExpenseSyncEnqueuer
import com.smartexpense.data.sync.RemoteDataSync
import com.smartexpense.database.ExpenseDeletionTransaction
import com.smartexpense.database.dao.ExpenseDao
import com.smartexpense.database.dao.PendingExpenseDeletionDao
import com.smartexpense.database.entity.SyncStatus
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.model.SearchCriteria
import com.smartexpense.domain.repository.CategoryRepository
import com.smartexpense.domain.repository.ExpenseRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.coroutines.cancellation.CancellationException

class ExpenseRepositoryImpl(
    private val expenseDao: ExpenseDao,
    private val supabaseExpenseRemoteDataSource: SupabaseExpenseRemoteDataSource,
    private val supabaseClient: SupabaseClient,
    private val categoryRepository: CategoryRepository,
    private val expenseSyncEnqueuer: ExpenseSyncEnqueuer,
    private val pendingExpenseDeletionDao: PendingExpenseDeletionDao,
    private val deletionTransaction: ExpenseDeletionTransaction,
    private val remoteDataSync: RemoteDataSync
) : ExpenseRepository {

    override fun getExpense(): Flow<List<Expense>> =
        expenseDao.getExpenses().map { expenses ->
            expenses.map { it.toDomain() }
        }

    override suspend fun getExpenseById(id: Long): Expense? =
        expenseDao.getExpenseById(id)?.toDomain()

    override suspend fun addExpense(expense: Expense) {
        expenseDao.insertExpense(expense.toEntity())
        expenseSyncEnqueuer.enqueue()
    }

    override suspend fun updateExpense(expense: Expense) {
        val existingEntity = expenseDao.getExpenseById(expense.id)
            ?: return

        expenseDao.updateExpense(
            existingEntity.copy(
                amount = expense.amount,
                categoryId = expense.categoryId,
                description = expense.description,
                date = expense.date,
                syncStatus = SyncStatus.PENDING
            )
        )

        expenseSyncEnqueuer.enqueue()
    }

    override suspend fun deleteExpense(expense: Expense) {
        val existingEntity = expenseDao.getExpenseById(expense.id)
            ?: return

        deletionTransaction.deleteExpense(
            expenseId = existingEntity.id,
            clientId = existingEntity.clientId
        )

        expenseSyncEnqueuer.enqueue()
    }

    override suspend fun syncPendingExpenses() {
        val userId = supabaseClient.auth.currentUserOrNull()?.id
            ?: return

        syncPendingExpensesForUser(userId)
    }

    internal suspend fun syncPendingExpensesForUser(userId: String) {
        // Pull the user's remote data into Room first so a fresh install (or a
        // new device) restores what they previously saved to Supabase.
        remoteDataSync.pullRemoteData(userId)

        categoryRepository.syncCategories()

        var hasFailures = false

        for (deletion in pendingExpenseDeletionDao.getPendingDeletions()) {
            try {
                supabaseExpenseRemoteDataSource.deleteExpenseByClientId(
                    deletion.clientId
                )

                pendingExpenseDeletionDao.deleteByClientId(deletion.clientId)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                hasFailures = true
            }
        }

        val pendingExpenses = expenseDao.getUnsyncedExpenses()

        for (expense in pendingExpenses) {
            try {
                val remoteCategoryId =
                    categoryRepository.getRemoteCategoryId(expense.categoryId)
                        ?: throw IllegalStateException(
                            "Could not resolve remote category ID"
                        )

                val dto = SupabaseExpenseDto(
                    clientId = expense.clientId,
                    userId = userId,
                    amount = expense.amount,
                    categoryId = remoteCategoryId,
                    description = expense.description,
                    expenseDate = java.time.Instant
                        .ofEpochMilli(expense.date)
                        .toString()
                )

                supabaseExpenseRemoteDataSource.insertExpense(dto)

                expenseDao.updateSyncStatus(
                    expenseId = expense.id,
                    status = SyncStatus.SYNCED.name
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                expenseDao.updateSyncStatus(
                    expenseId = expense.id,
                    status = SyncStatus.FAILED.name
                )
                hasFailures = true
            }
        }

        if (hasFailures) {
            throw IllegalStateException(
                "Some expenses failed to synchronize"
            )
        }
    }

    override fun searchExpenses(criteria: SearchCriteria): Flow<List<Expense>> {
        val normalized = criteria.normalize()
        val query = normalized.query.takeIf { it.isNotEmpty() }
        val categoryIdList = normalized.selectedCategoryIds.toList()

        return expenseDao.searchExpenses(
            query = query,
            categoryIds = categoryIdList,
            categoryIdCount = categoryIdList.size,
            minAmount = normalized.minAmount,
            maxAmount = normalized.maxAmount,
            startTime = normalized.startTime,
            endTime = normalized.endTime,
            sortOrder = normalized.sortOrder.name
        ).map { expenses ->
            expenses.map { it.toDomain() }
        }
    }
}