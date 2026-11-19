package com.smartexpense.data.sync

import android.util.Log
import com.smartexpense.data.remote.SupabaseBillRemoteDataSource
import com.smartexpense.data.remote.SupabaseBudgetRemoteDataSource
import com.smartexpense.data.remote.SupabaseCategoryRemoteDataSource
import com.smartexpense.data.remote.SupabaseExpenseRemoteDataSource
import com.smartexpense.data.remote.SupabasePaycheckRemoteDataSource
import com.smartexpense.database.dao.BillDao
import com.smartexpense.database.dao.BillPaymentDao
import com.smartexpense.database.dao.BudgetDao
import com.smartexpense.database.dao.CategoryDao
import com.smartexpense.database.dao.ExpenseDao
import com.smartexpense.database.dao.PaycheckBillDao
import com.smartexpense.database.dao.PaycheckDao
import com.smartexpense.database.dao.PaycheckScheduleDao
import com.smartexpense.database.dao.PendingBillDeletionDao
import com.smartexpense.database.dao.PendingBudgetDeletionDao
import com.smartexpense.database.dao.PendingExpenseDeletionDao
import com.smartexpense.database.dao.PendingPaycheckDeletionDao
import com.smartexpense.database.entity.BillEntity
import com.smartexpense.database.entity.BillPaymentEntity
import com.smartexpense.database.entity.BudgetEntity
import com.smartexpense.database.entity.CategoryEntity
import com.smartexpense.database.entity.ExpenseEntity
import com.smartexpense.database.entity.PaycheckBillEntity
import com.smartexpense.database.entity.PaycheckEntity
import com.smartexpense.database.entity.PaycheckScheduleEntity
import com.smartexpense.database.entity.SyncStatus
import com.smartexpense.domain.model.PaycheckFrequency
import com.smartexpense.domain.money.Money
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
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
    private val paycheckDao: PaycheckDao,
    private val paycheckBillDao: PaycheckBillDao,
    private val paycheckScheduleDao: PaycheckScheduleDao,
    private val pendingPaycheckDeletionDao: PendingPaycheckDeletionDao,
    private val categoryRemoteDataSource: SupabaseCategoryRemoteDataSource,
    private val expenseRemoteDataSource: SupabaseExpenseRemoteDataSource,
    private val budgetRemoteDataSource: SupabaseBudgetRemoteDataSource,
    private val billRemoteDataSource: SupabaseBillRemoteDataSource,
    private val paycheckRemoteDataSource: SupabasePaycheckRemoteDataSource
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

        // Paychecks reference bills and their recurring schedule, so restore
        // the schedules first.
        try {
            pullRemotePaycheckSchedules(userId)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to restore paycheck schedules", exception)
        }

        try {
            pullRemotePaychecks(userId)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to restore paychecks", exception)
        }

        try {
            pullRemotePaycheckBills(userId)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to restore paycheck bill assignments", exception)
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
                        repeatDayOfMonth = if (remote.frequency == "NONE") {
                            null
                        } else {
                            Instant.ofEpochMilli(dueDate)
                                .atZone(ZoneId.systemDefault())
                                .dayOfMonth
                        },
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

    private suspend fun pullRemotePaycheckSchedules(userId: String) {
        val remoteSchedules = paycheckRemoteDataSource.getPaycheckSchedules(userId)
        if (remoteSchedules.isEmpty()) return
        val existingClientIds = paycheckScheduleDao.getAllClientIds().toSet()

        for (remote in remoteSchedules) {
            if (remote.clientId in existingClientIds) continue
            val frequency = PaycheckFrequency.entries
                .firstOrNull { it.name == remote.frequency }
                ?: continue
            val startDate = parseTimestamp(remote.startDate) ?: continue
            val endDate = remote.endDate?.let { parseTimestamp(it) }
            val createdAt = parseTimestamp(remote.createdAt) ?: startDate

            try {
                paycheckScheduleDao.insertScheduleIgnoringDuplicate(
                    PaycheckScheduleEntity(
                        userId = userId,
                        clientId = remote.clientId,
                        syncStatus = SyncStatus.SYNCED,
                        amount = remote.amount,
                        label = remote.label,
                        frequency = frequency.name,
                        firstDay = remote.firstDay,
                        secondDay = remote.secondDay,
                        startDate = startDate,
                        endDate = endDate,
                        isActive = remote.isActive,
                        createdAt = createdAt
                    )
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to restore paycheck schedule ${remote.clientId}", exception)
            }
        }
    }

    private suspend fun pullRemotePaychecks(userId: String) {
        val remotePaychecks = paycheckRemoteDataSource.getPaychecks(userId)
        if (remotePaychecks.isEmpty()) return
        val pendingDeletionClientIds = pendingPaycheckDeletionDao
            .getPendingDeletions()
            .map { it.clientId }
            .toSet()
        for (remote in remotePaychecks) {
            if (remote.clientId in pendingDeletionClientIds) continue
            if (paycheckDao.getPaycheckByClientId(remote.clientId) != null) continue
            val payDate = parseTimestamp(remote.payDate) ?: continue
            val createdAt = remote.createdAt.let { parseTimestamp(it) } ?: payDate
            try {
                paycheckDao.insertPaycheck(
                    PaycheckEntity(
                        userId = userId,
                        clientId = remote.clientId,
                        syncStatus = SyncStatus.SYNCED,
                        amount = remote.amount,
                        payDate = payDate,
                        label = remote.label,
                        createdAt = createdAt,
                        scheduleClientId = remote.scheduleClientId,
                        occurrenceDate = remote.occurrenceDate?.let {
                            parseTimestamp(it)
                        },
                        isSkipped = remote.isSkipped
                    )
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to restore paycheck ${remote.clientId}", exception)
            }
        }
    }

    private suspend fun pullRemotePaycheckBills(userId: String) {
        val remoteAssignments = paycheckRemoteDataSource.getPaycheckBills(userId)
        if (remoteAssignments.isEmpty()) return
        for (remote in remoteAssignments) {
            // Both the paycheck and the bill must exist locally before the
            // assignment can be represented.
            val paycheck = paycheckDao.getPaycheckByClientId(
                remote.paycheckClientId
            ) ?: continue
            // A paycheck with unsynced local changes is about to be uploaded
            // together with its own selection, which replaces the remote set.
            // Restoring remote assignments here would resurrect bills the
            // user just removed.
            if (paycheck.syncStatus != SyncStatus.SYNCED) continue
            val bill = billDao.getBillByClientId(remote.billClientId) ?: continue
            try {
                paycheckBillDao.insertAssignment(
                    PaycheckBillEntity(
                        userId = userId,
                        paycheckId = paycheck.id,
                        billId = bill.id,
                        clientId = remote.clientId,
                        paycheckClientId = remote.paycheckClientId,
                        billClientId = remote.billClientId,
                        createdAt = parseTimestamp(remote.createdAt)
                            ?: paycheck.createdAt
                    )
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(
                    TAG,
                    "Failed to restore paycheck bill ${remote.clientId}",
                    exception
                )
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
