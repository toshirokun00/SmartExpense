package com.smartexpense.data.repositoryimpl

import com.smartexpense.data.mapper.toDomain
import com.smartexpense.data.mapper.toEntity
import com.smartexpense.data.remote.SupabaseBillDto
import com.smartexpense.data.remote.SupabaseBillPaymentDto
import com.smartexpense.data.remote.SupabaseBillRemoteDataSource
import com.smartexpense.data.sync.ExpenseSyncEnqueuer
import com.smartexpense.database.BillDeletionTransaction
import com.smartexpense.database.dao.BillDao
import com.smartexpense.database.dao.BillPaymentDao
import com.smartexpense.database.dao.PendingBillDeletionDao
import com.smartexpense.database.entity.BillPaymentEntity
import com.smartexpense.database.entity.SyncStatus
import com.smartexpense.domain.model.Bill
import com.smartexpense.domain.model.BillAmountType
import com.smartexpense.domain.model.BillPayment
import com.smartexpense.domain.model.PaymentMethod
import com.smartexpense.domain.repository.AuthRepository
import com.smartexpense.domain.repository.BillRepository
import com.smartexpense.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.round

class BillRepositoryImpl(
    private val billDao: BillDao,
    private val billPaymentDao: BillPaymentDao,
    private val supabaseBillRemoteDataSource: SupabaseBillRemoteDataSource,
    private val categoryRepository: CategoryRepository,
    private val expenseSyncEnqueuer: ExpenseSyncEnqueuer,
    private val pendingBillDeletionDao: PendingBillDeletionDao,
    private val billDeletionTransaction: BillDeletionTransaction,
    private val authRepository: AuthRepository,
    private val clock: Clock
) : BillRepository {

    override fun getBills(): Flow<List<Bill>> = flow {
        val userId = authRepository.getCurrentUserId()
        if (userId == null) {
            emit(emptyList())
            return@flow
        }
        billDao.getBillsForUser(userId).collect { entities ->
            emit(entities.map { it.toDomain() })
        }
    }

    override fun getPayments(billId: Long): Flow<List<BillPayment>> =
        billPaymentDao.getPaymentsForBill(billId)
            .map { entities -> entities.map { it.toDomain() } }

    override fun getPaymentHistory(): Flow<List<BillPayment>> = flow {
        val userId = authRepository.getCurrentUserId()
        if (userId == null) {
            emit(emptyList())
            return@flow
        }
        billPaymentDao.getPaymentsForUser(userId).collect { entities ->
            emit(entities.map { it.toDomain() })
        }
    }

    override suspend fun getBill(id: Long): Bill? {
        val userId = authRepository.getCurrentUserId() ?: return null
        val entity = billDao.getBillById(id) ?: return null
        return if (entity.userId == userId) entity.toDomain() else null
    }

    override suspend fun addBill(bill: Bill): Long {
        val userId = authRepository.getCurrentUserId()
            ?: throw IllegalStateException("Sign in to add bills")
        val id = billDao.insertBill(
            bill.toEntity(userId).copy(syncStatus = SyncStatus.PENDING)
        )
        expenseSyncEnqueuer.enqueue()
        return id
    }

    override suspend fun updateBill(bill: Bill) {
        val userId = authRepository.getCurrentUserId()
            ?: throw IllegalStateException("Sign in to update bills")
        val existing = billDao.getBillById(bill.id)
            ?: throw IllegalArgumentException("Bill not found")
        if (existing.userId != userId) throw IllegalArgumentException("Bill not found")
        billDao.updateBill(
            bill.toEntity(userId).copy(
                id = bill.id,
                clientId = existing.clientId,
                syncStatus = SyncStatus.PENDING
            )
        )
        expenseSyncEnqueuer.enqueue()
    }

    override suspend fun deleteBill(id: Long) {
        val userId = authRepository.getCurrentUserId() ?: return
        val existing = billDao.getBillById(id) ?: return
        if (existing.userId != userId) return

        billDeletionTransaction.deleteBill(
            billId = existing.id,
            clientId = existing.clientId
        )
        expenseSyncEnqueuer.enqueue()
    }

    override suspend fun payBill(
        id: Long,
        amountPaid: Double,
        paidAtMillis: Long,
        paymentMethod: PaymentMethod
    ) {
        val userId = authRepository.getCurrentUserId() ?: return
        val existing = billDao.getBillById(id) ?: return
        if (existing.userId != userId) return

        billPaymentDao.insertPayment(
            BillPaymentEntity(
                userId = userId,
                billId = existing.id,
                billName = existing.name,
                amount = amountPaid,
                paidAt = paidAtMillis,
                paymentMethod = paymentMethod.name,
                createdAt = clock.millis()
            )
        )

        val bill = existing.toDomain()
        val nextDueDate = bill.nextDueDateMillis(clock.zone)

        if (nextDueDate != null) {
            // Recurring: record this occurrence, then move to the next cycle and
            // leave the bill unpaid again.
            val nextAmount = if (bill.amountType == BillAmountType.VARIABLE) {
                averageRecentPayment(existing.id) ?: amountPaid
            } else {
                bill.amount
            }
            billDao.updateBill(
                existing.copy(
                    dueDate = nextDueDate,
                    amount = nextAmount,
                    amountEstimated = bill.amountType == BillAmountType.VARIABLE,
                    isPaid = false,
                    paidAt = null,
                    skipped = false,
                    syncStatus = SyncStatus.PENDING
                )
            )
        } else {
            billDao.updateBill(
                existing.copy(
                    amount = amountPaid,
                    amountEstimated = false,
                    isPaid = true,
                    paidAt = paidAtMillis,
                    skipped = false,
                    syncStatus = SyncStatus.PENDING
                )
            )
        }

        expenseSyncEnqueuer.enqueue()
    }

    override suspend fun skipBill(id: Long) {
        val userId = authRepository.getCurrentUserId() ?: return
        val existing = billDao.getBillById(id) ?: return
        if (existing.userId != userId) return

        val bill = existing.toDomain()
        val nextDueDate = bill.nextDueDateMillis(clock.zone)
        if (nextDueDate != null) {
            billDao.updateBill(
                existing.copy(
                    dueDate = nextDueDate,
                    isPaid = false,
                    paidAt = null,
                    skipped = false,
                    syncStatus = SyncStatus.PENDING
                )
            )
        } else {
            billDao.updateBill(
                existing.copy(
                    isPaid = false,
                    paidAt = null,
                    skipped = true,
                    syncStatus = SyncStatus.PENDING
                )
            )
        }

        expenseSyncEnqueuer.enqueue()
    }

    override suspend fun syncBills(userId: String) {
        var failed = false

        // Apply queued deletions first so a deleted bill is not re-created by
        // the pull that runs before this push.
        for (deletion in pendingBillDeletionDao.getPendingDeletions()) {
            try {
                supabaseBillRemoteDataSource.deleteBillByClientId(
                    deletion.clientId
                )
                pendingBillDeletionDao.deleteByClientId(deletion.clientId)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                failed = true
            }
        }

        for (bill in billDao.getUnsyncedBills()) {
            // Never upload one user's pending rows under another account.
            if (bill.userId != userId) continue

            try {
                val remoteCategoryId = bill.categoryId?.let { localCategoryId ->
                    categoryRepository.getRemoteCategoryId(localCategoryId)
                }

                supabaseBillRemoteDataSource.upsertBill(
                    SupabaseBillDto(
                        clientId = bill.clientId,
                        userId = userId,
                        name = bill.name,
                        amount = bill.amount,
                        dueDate = Instant.ofEpochMilli(bill.dueDate).toString(),
                        categoryId = remoteCategoryId,
                        notes = bill.notes,
                        frequency = bill.frequency,
                        repeatInterval = bill.repeatInterval,
                        amountType = bill.amountType,
                        amountEstimated = bill.amountEstimated,
                        payee = bill.payee,
                        paymentMethod = bill.paymentMethod,
                        autopay = bill.autopay,
                        reminderDaysBefore = bill.reminderDaysBefore,
                        reminderTimeMinutes = bill.reminderTimeMinutes,
                        paidAt = bill.paidAt?.let {
                            Instant.ofEpochMilli(it).toString()
                        },
                        skipped = bill.skipped,
                        isActive = bill.isActive
                    )
                )

                billDao.updateSyncStatus(
                    billId = bill.id,
                    status = SyncStatus.SYNCED.name
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                billDao.updateSyncStatus(
                    billId = bill.id,
                    status = SyncStatus.FAILED.name
                )
                failed = true
            }
        }

        for (payment in billPaymentDao.getUnsyncedPayments()) {
            if (payment.userId != userId) continue

            // The remote row references the bill; wait until its parent has
            // been uploaded so the foreign key is valid.
            val bill = billDao.getBillById(payment.billId) ?: continue
            if (bill.syncStatus != SyncStatus.SYNCED) continue

            try {
                supabaseBillRemoteDataSource.upsertBillPayment(
                    SupabaseBillPaymentDto(
                        clientId = payment.clientId,
                        userId = userId,
                        billClientId = bill.clientId,
                        billName = payment.billName,
                        amount = payment.amount,
                        paidAt = Instant.ofEpochMilli(payment.paidAt).toString(),
                        paymentMethod = payment.paymentMethod,
                        createdAt = Instant.ofEpochMilli(payment.createdAt).toString()
                    )
                )

                billPaymentDao.updateSyncStatus(
                    paymentId = payment.id,
                    status = SyncStatus.SYNCED.name
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                billPaymentDao.updateSyncStatus(
                    paymentId = payment.id,
                    status = SyncStatus.FAILED.name
                )
                failed = true
            }
        }

        if (failed) {
            error("Some bills failed to synchronize")
        }
    }

    private suspend fun averageRecentPayment(billId: Long): Double? {
        val recent = billPaymentDao.getRecentPaymentsForBill(billId, RECENT_PAYMENT_WINDOW)
        if (recent.isEmpty()) return null
        val average = recent.map { it.amount }.average()
        return round(average * 100.0) / 100.0
    }

    private companion object {
        const val RECENT_PAYMENT_WINDOW = 6
    }
}
