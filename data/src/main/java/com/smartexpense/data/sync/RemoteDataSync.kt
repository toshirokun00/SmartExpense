package com.smartexpense.data.sync

import android.util.Log
import com.smartexpense.data.remote.SupabaseCategoryRemoteDataSource
import com.smartexpense.data.remote.SupabaseExpenseRemoteDataSource
import com.smartexpense.database.dao.CategoryDao
import com.smartexpense.database.dao.ExpenseDao
import com.smartexpense.database.dao.PendingExpenseDeletionDao
import com.smartexpense.database.entity.CategoryEntity
import com.smartexpense.database.entity.ExpenseEntity
import com.smartexpense.database.entity.SyncStatus
import java.time.OffsetDateTime
import kotlin.coroutines.cancellation.CancellationException

/**
 * Pulls the signed-in user's data from Supabase back into the local Room database.
 *
 * This is what restores a user's data after a reinstall or when they sign in on a new
 * device. It is intentionally additive and idempotent:
 *
 *  - Rows are matched by [clientId], so re-running never creates duplicates.
 *  - Existing local rows are never overwritten or deleted (local state wins).
 *  - Expenses whose category is unknown locally are skipped.
 *  - Expenses that still have a queued local deletion are skipped so they are not
 *    resurrected on the device that deleted them.
 */
class RemoteDataSync(
    private val categoryDao: CategoryDao,
    private val expenseDao: ExpenseDao,
    private val pendingExpenseDeletionDao: PendingExpenseDeletionDao,
    private val categoryRemoteDataSource: SupabaseCategoryRemoteDataSource,
    private val expenseRemoteDataSource: SupabaseExpenseRemoteDataSource
) {

    suspend fun pullRemoteData(userId: String) {
        val remoteCategoryIdToLocalId = pullRemoteCategories(userId)

        pullRemoteExpenses(
            userId = userId,
            remoteCategoryIdToLocalId = remoteCategoryIdToLocalId
        )
    }

    private suspend fun pullRemoteCategories(
        userId: String
    ): Map<Long, Long> {
        val remoteCategories = categoryRemoteDataSource.getCategories(userId)

        if (remoteCategories.isEmpty()) return emptyMap()

        val existingClientIds = categoryDao.getCategoriesOnce()
            .map { it.clientId }
            .toSet()

        for (remoteCategory in remoteCategories) {
            if (remoteCategory.clientId in existingClientIds) continue

            try {
                categoryDao.insertCategory(
                    CategoryEntity(
                        name = remoteCategory.name,
                        clientId = remoteCategory.clientId
                    )
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(
                    TAG,
                    "Failed to restore category ${remoteCategory.clientId}",
                    exception
                )
            }
        }

        val localIdByClientId = categoryDao.getCategoriesOnce()
            .associate { it.clientId to it.id }

        return remoteCategories
            .mapNotNull { remoteCategory ->
                localIdByClientId[remoteCategory.clientId]?.let { localId ->
                    remoteCategory.id to localId
                }
            }
            .toMap()
    }

    private suspend fun pullRemoteExpenses(
        userId: String,
        remoteCategoryIdToLocalId: Map<Long, Long>
    ) {
        val remoteExpenses = expenseRemoteDataSource.getExpenses(userId)

        if (remoteExpenses.isEmpty()) return

        val localClientIds = expenseDao.getAllClientIds().toSet()
        val pendingDeletionClientIds = pendingExpenseDeletionDao
            .getPendingDeletions()
            .map { it.clientId }
            .toSet()

        for (remoteExpense in remoteExpenses) {
            if (remoteExpense.clientId in localClientIds) continue
            if (remoteExpense.clientId in pendingDeletionClientIds) continue

            val localCategoryId =
                remoteCategoryIdToLocalId[remoteExpense.categoryId]

            if (localCategoryId == null) {
                Log.w(
                    TAG,
                    "Skipping expense ${remoteExpense.clientId}: " +
                        "unknown category ${remoteExpense.categoryId}"
                )
                continue
            }

            val date = parseExpenseDate(remoteExpense.expenseDate)

            if (date == null) {
                Log.w(
                    TAG,
                    "Skipping expense ${remoteExpense.clientId}: " +
                        "unparsable date ${remoteExpense.expenseDate}"
                )
                continue
            }

            try {
                expenseDao.insertExpense(
                    ExpenseEntity(
                        amount = remoteExpense.amount,
                        categoryId = localCategoryId,
                        description = remoteExpense.description,
                        date = date,
                        syncStatus = SyncStatus.SYNCED,
                        clientId = remoteExpense.clientId,
                        userId = userId
                    )
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(
                    TAG,
                    "Failed to restore expense ${remoteExpense.clientId}",
                    exception
                )
            }
        }
    }

    private fun parseExpenseDate(value: String): Long? {
        return try {
            OffsetDateTime.parse(value).toInstant().toEpochMilli()
        } catch (exception: Exception) {
            Log.w(TAG, "Unable to parse remote expense date: $value", exception)
            null
        }
    }

    private companion object {
        const val TAG = "RemoteDataSync"
    }
}
