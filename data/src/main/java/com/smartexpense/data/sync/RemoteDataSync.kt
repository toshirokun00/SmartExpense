package com.smartexpense.data.sync

import android.util.Log
import com.smartexpense.data.remote.SupabaseBillRemoteDataSource
import com.smartexpense.data.remote.SupabaseBudgetRemoteDataSource
import com.smartexpense.data.remote.SupabaseCategoryRemoteDataSource
import com.smartexpense.data.remote.SupabaseExpenseRemoteDataSource
import com.smartexpense.database.dao.BillDao
import com.smartexpense.database.dao.BillPaymentDao
import com.smartexpense.database.dao.BudgetDao
import com.smartexpense.database.dao.CategoryDao
import com.smartexpense.database.dao.ExpenseDao
import com.smartexpense.database.dao.PendingBillDeletionDao
import com.smartexpense.database.dao.PendingBudgetDeletionDao
import com.smartexpense.database.dao.PendingExpenseDeletionDao
import com.smartexpense.database.entity.BillEntity
import com.smartexpense.database.entity.BillPaymentEntity
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
    private val billDao: BillDao,
    private val billPaymentDao: BillPaymentDao,
    private val pendingExpenseDeletionDao: PendingExpenseDeletionDao,
    private val pendingBudgetDeletionDao: PendingBudgetDeletionDao,
    private val pendingBillDeletionDao: PendingBillDeletionDao,
    private val categoryRemoteDataSource: SupabaseCategoryRemoteDataSource,
    private val expenseRemoteDataSource: SupabaseExpenseRemoteDataSource,
    private val budgetRemoteDataSource: SupabaseBudgetRemoteDataSource,
    private val billRemoteDataSource: SupabaseBillRemoteDataSource
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

        // Bills are isolated separately so a missing bills table does not
        // prevent expenses and budgets from restoring.
        try {
            pullRemoteBills(userId, remoteCategoryIdToLocalId)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to restore bills", exception)
        }

        try {
            pullRemoteBillPayments(userId)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to restore bill payments", exception)
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
                        paymentMethod = remote.paymentMethod ?: "CASH",
                        expenseType = remote.expenseType ?: "EXPENSE",
                        tags = remote.tags.orEmpty(),
                        notes = remote.notes.orEmpty(),
                        receiptPhotoUri = remote.receiptPhotoUri,
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

    private suspend fun pullRemoteBills(userId: String, categoryIds: Map<Long, Long>) {
        val remoteBills = billRemoteDataSource.getBills(userId)
        if (remoteBills.isEmpty()) return
        val existingClientIds = billDao.getAllClientIds().toSet()
        val pendingDeletionClientIds = pendingBillDeletionDao
            .getPendingDeletions()
            .map { it.clientId }
            .toSet()
        for (remote in remoteBills) {
            if (remote.clientId in existingClientIds) continue
            if (remote.clientId in pendingDeletionClientIds) continue
            val dueDate = parseTimestamp(remote.dueDate) ?: continue
            val paidAt = remote.paidAt?.let { parseTimestamp(it) }
            try {
                billDao.insertBill(
                    BillEntity(
                        userId = userId,
                        clientId = remote.clientId,
                        syncStatus = SyncStatus.SYNCED,
                        name = remote.name,
                        amount = remote.amount,
                        dueDate = dueDate,
                        isPaid = paidAt != null,
                        notes = remote.notes,
                        frequency = remote.frequency,
                        repeatInterval = remote.repeatInterval,
                        amountType = remote.amountType,
                        amountEstimated = remote.amountEstimated,
                        categoryId = remote.categoryId?.let { categoryIds[it] },
                        payee = remote.payee,
                        paymentMethod = remote.paymentMethod,
                        autopay = remote.autopay,
                        reminderDaysBefore = remote.reminderDaysBefore,
                        reminderTimeMinutes = remote.reminderTimeMinutes,
                        paidAt = paidAt,
                        skipped = remote.skipped,
                        isActive = remote.isActive
                    )
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to restore bill ${remote.clientId}", exception)
            }
        }
    }

    private suspend fun pullRemoteBillPayments(userId: String) {
        val remotePayments = billRemoteDataSource.getBillPayments(userId)
        if (remotePayments.isEmpty()) return
        val existingClientIds = billPaymentDao.getAllClientIds().toSet()
        for (remote in remotePayments) {
            if (remote.clientId in existingClientIds) continue
            // The payment references its bill by remote client id, so the bill
            // must already be restored locally.
            val bill = billDao.getBillByClientId(remote.billClientId) ?: continue
            val paidAt = parseTimestamp(remote.paidAt) ?: continue
            val createdAt = parseTimestamp(remote.createdAt) ?: paidAt
            try {
                billPaymentDao.insertPayment(
                    BillPaymentEntity(
                        userId = userId,
                        billId = bill.id,
                        clientId = remote.clientId,
                        syncStatus = SyncStatus.SYNCED,
                        billName = remote.billName,
                        amount = remote.amount,
                        paidAt = paidAt,
                        paymentMethod = remote.paymentMethod,
                        createdAt = createdAt
                    )
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to restore bill payment ${remote.clientId}", exception)
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
