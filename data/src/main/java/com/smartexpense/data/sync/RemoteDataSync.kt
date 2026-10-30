package com.smartexpense.data.sync

import android.util.Log
import com.smartexpense.data.remote.SupabaseBudgetRemoteDataSource
import com.smartexpense.data.remote.SupabaseCategoryRemoteDataSource
import com.smartexpense.data.remote.SupabaseExpenseRemoteDataSource
import com.smartexpense.database.dao.BudgetDao
import com.smartexpense.database.dao.CategoryDao
import com.smartexpense.database.dao.ExpenseDao
import com.smartexpense.database.dao.PendingBudgetDeletionDao
import com.smartexpense.database.dao.PendingExpenseDeletionDao
import com.smartexpense.database.entity.BudgetEntity
import com.smartexpense.database.entity.CategoryEntity
import com.smartexpense.database.entity.ExpenseEntity
import com.smartexpense.database.entity.SyncStatus
import com.smartexpense.domain.money.Money
import java.time.OffsetDateTime
import kotlin.coroutines.cancellation.CancellationException

/** Restores the signed-in user's remote data into Room on login and new installs. */
class RemoteDataSync(
    private val categoryDao: CategoryDao,
    private val expenseDao: ExpenseDao,
    private val budgetDao: BudgetDao,
    private val pendingExpenseDeletionDao: PendingExpenseDeletionDao,
    private val pendingBudgetDeletionDao: PendingBudgetDeletionDao,
    private val categoryRemoteDataSource: SupabaseCategoryRemoteDataSource,
    private val expenseRemoteDataSource: SupabaseExpenseRemoteDataSource,
    private val budgetRemoteDataSource: SupabaseBudgetRemoteDataSource
) {
    suspend fun pullRemoteData(userId: String) {
        val remoteCategoryIdToLocalId = pullRemoteCategories(userId)
        pullRemoteExpenses(userId, remoteCategoryIdToLocalId)

        // Budget sync is newer than the other entities. A missing table or a
        // policy problem must not stop categories and expenses from syncing.
        try {
            pullRemoteBudgets(userId, remoteCategoryIdToLocalId)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to restore budgets", exception)
        }
    }

    private suspend fun pullRemoteCategories(userId: String): Map<Long, Long> {
        val remoteCategories = categoryRemoteDataSource.getCategories(userId)
        if (remoteCategories.isEmpty()) return emptyMap()
        val existingClientIds = categoryDao.getCategoriesOnce().map { it.clientId }.toSet()
        for (remoteCategory in remoteCategories) {
            if (remoteCategory.clientId in existingClientIds) continue
            try {
                categoryDao.insertCategory(
                    CategoryEntity(name = remoteCategory.name, clientId = remoteCategory.clientId)
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to restore category ${remoteCategory.clientId}", exception)
            }
        }
        val localIdByClientId = categoryDao.getCategoriesOnce().associate { it.clientId to it.id }
        return remoteCategories.mapNotNull { remote ->
            localIdByClientId[remote.clientId]?.let { remote.id to it }
        }.toMap()
    }

    private suspend fun pullRemoteExpenses(userId: String, categoryIds: Map<Long, Long>) {
        val remoteExpenses = expenseRemoteDataSource.getExpenses(userId)
        if (remoteExpenses.isEmpty()) return
        val localClientIds = expenseDao.getAllClientIds().toSet()
        val pendingDeletionClientIds = pendingExpenseDeletionDao.getPendingDeletions()
            .map { it.clientId }.toSet()
        for (remote in remoteExpenses) {
            if (remote.clientId in localClientIds || remote.clientId in pendingDeletionClientIds) continue
            val localCategoryId = categoryIds[remote.categoryId] ?: continue
            val date = parseTimestamp(remote.expenseDate) ?: continue
            try {
                expenseDao.insertExpense(
                    ExpenseEntity(
                        amountMinor = Money.toMinorUnits(remote.amount),
                        categoryId = localCategoryId,
                        description = remote.description,
                        date = date,
                        merchant = remote.merchant.orEmpty(),
                        syncStatus = SyncStatus.SYNCED,
                        clientId = remote.clientId,
                        userId = userId
                    )
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to restore expense ${remote.clientId}", exception)
            }
        }
    }

    private suspend fun pullRemoteBudgets(userId: String, categoryIds: Map<Long, Long>) {
        val remoteBudgets = budgetRemoteDataSource.getBudgets(userId)
        if (remoteBudgets.isEmpty()) return
        val existingClientIds = budgetDao.getBudgetsOnce().map { it.clientId }.toSet()
        val pendingDeletionClientIds = pendingBudgetDeletionDao
            .getPendingDeletions()
            .map { it.clientId }
            .toSet()
        for (remote in remoteBudgets) {
            if (remote.clientId in existingClientIds) continue
            if (remote.clientId in pendingDeletionClientIds) continue
            val localCategoryId = categoryIds[remote.categoryId] ?: continue
            val startDate = parseTimestamp(remote.startDate) ?: continue
            val endDate = parseTimestamp(remote.endDate) ?: continue
            try {
                budgetDao.insertBudgetIgnoringDuplicate(
                    BudgetEntity(
                        categoryId = localCategoryId,
                        amount = remote.amount,
                        startDate = startDate,
                        endDate = endDate,
                        clientId = remote.clientId,
                        userId = userId,
                        syncStatus = SyncStatus.SYNCED
                    )
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to restore budget ${remote.clientId}", exception)
            }
        }
    }

    private fun parseTimestamp(value: String): Long? = try {
        OffsetDateTime.parse(value).toInstant().toEpochMilli()
    } catch (exception: Exception) {
        Log.w(TAG, "Unable to parse remote timestamp: $value", exception)
        null
    }

    private companion object {
        const val TAG = "RemoteDataSync"
    }
}
