package com.smartexpense.data.repositoryimpl

import com.smartexpense.data.mapper.toDomain
import com.smartexpense.data.mapper.toEntity
import com.smartexpense.data.remote.SupabasePaycheckBillDto
import com.smartexpense.data.remote.SupabasePaycheckDto
import com.smartexpense.data.remote.SupabasePaycheckRemoteDataSource
import com.smartexpense.data.remote.SupabasePaycheckScheduleDto
import com.smartexpense.data.sync.ExpenseSyncEnqueuer
import com.smartexpense.database.dao.BillDao
import com.smartexpense.database.dao.PaycheckBillDao
import com.smartexpense.database.dao.PaycheckDao
import com.smartexpense.database.dao.PaycheckScheduleDao
import com.smartexpense.database.dao.PendingPaycheckDeletionDao
import com.smartexpense.database.entity.PaycheckBillEntity
import com.smartexpense.database.entity.PaycheckEntity
import com.smartexpense.database.entity.PaycheckScheduleEntity
import com.smartexpense.database.entity.PendingPaycheckDeletionEntity
import com.smartexpense.database.entity.SyncStatus
import com.smartexpense.domain.model.Paycheck
import com.smartexpense.domain.model.PaycheckSchedule
import com.smartexpense.domain.model.paycheckOccurrenceClientId
import com.smartexpense.domain.repository.AuthRepository
import com.smartexpense.domain.repository.PaycheckRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class PaycheckRepositoryImpl(
    private val paycheckDao: PaycheckDao,
    private val paycheckBillDao: PaycheckBillDao,
    private val paycheckScheduleDao: PaycheckScheduleDao,
    private val pendingPaycheckDeletionDao: PendingPaycheckDeletionDao,
    private val supabasePaycheckRemoteDataSource: SupabasePaycheckRemoteDataSource,
    private val billDao: BillDao,
    private val expenseSyncEnqueuer: ExpenseSyncEnqueuer,
    private val authRepository: AuthRepository,
    private val clock: Clock
) : PaycheckRepository {

    override fun getPaychecks(): Flow<List<Paycheck>> = flow {
        val userId = authRepository.getCurrentUserId()
        if (userId == null) {
            emit(emptyList())
            return@flow
        }
        combine(
            paycheckDao.getPaychecksForUser(userId),
            paycheckBillDao.getAssignmentsForUser(userId)
        ) { paychecks, assignments ->
            val billIdsByPaycheck = assignments
                .groupBy { it.paycheckId }
                .mapValues { (_, rows) -> rows.map { it.billId } }
            paychecks.map { entity ->
                entity.toDomain(billIdsByPaycheck[entity.id].orEmpty())
            }
        }.collect { emit(it) }
    }

    override fun getPaycheckSchedules(): Flow<List<PaycheckSchedule>> = flow {
        val userId = authRepository.getCurrentUserId()
        if (userId == null) {
            emit(emptyList())
            return@flow
        }
        paycheckScheduleDao.getSchedulesForUser(userId)
            .map { schedules -> schedules.map { it.toDomain() } }
            .collect { emit(it) }
    }

    override suspend fun getPaycheck(id: Long): Paycheck? {
        val userId = authRepository.getCurrentUserId() ?: return null
        val entity = paycheckDao.getPaycheckById(id) ?: return null
        if (entity.userId != userId) return null
        val billIds = paycheckBillDao.getAssignmentsForPaycheck(id).map { it.billId }
        return entity.toDomain(billIds)
    }

    override suspend fun addPaycheck(paycheck: Paycheck): Long {
        val userId = authRepository.getCurrentUserId()
            ?: throw IllegalStateException("Sign in to add paychecks")
        val now = clock.millis()
        val entity = PaycheckEntity(
            userId = userId,
            amount = paycheck.amount,
            payDate = paycheck.payDateMillis,
            label = paycheck.label,
            createdAt = now
        )
        val paycheckId = paycheckDao.insertPaycheck(entity)

        insertAssignments(
            userId = userId,
            paycheckId = paycheckId,
            paycheckClientId = entity.clientId,
            billIds = paycheck.billIds
        )

        expenseSyncEnqueuer.enqueue()
        return paycheckId
    }

    override suspend fun addPaycheckSchedule(
        schedule: PaycheckSchedule,
        billIds: List<Long>
    ): Long {
        val userId = authRepository.getCurrentUserId()
            ?: throw IllegalStateException("Sign in to add paychecks")
        val now = clock.millis()
        val entity = schedule.toEntity(userId).copy(
            clientId = schedule.clientId.ifBlank { UUID.randomUUID().toString() },
            createdAt = now,
            syncStatus = SyncStatus.PENDING
        )
        val scheduleId = paycheckScheduleDao.insertScheduleIgnoringDuplicate(entity)
        if (scheduleId == -1L) {
            // The same client id is already stored, so reuse the existing row.
            return paycheckScheduleDao.getScheduleByClientId(entity.clientId)?.id ?: 0L
        }

        val today = LocalDate.now(clock)
        val created = materializeOccurrences(
            schedule = entity.copy(id = scheduleId),
            from = schedule.startDate(clock.zone),
            to = today.plusMonths(HORIZON_MONTHS)
        )

        // The bills chosen in the form belong to the first cutoff only. Later
        // cutoffs start empty, and their bills are picked from what is still
        // uncovered so the same bill is never planned twice.
        created.firstOrNull()?.let { first ->
            insertAssignments(
                userId = userId,
                paycheckId = first.id,
                paycheckClientId = first.clientId,
                billIds = billIds
            )
        }

        expenseSyncEnqueuer.enqueue()
        return scheduleId
    }

    override suspend fun stopPaycheckSchedule(scheduleClientId: String) {
        val userId = authRepository.getCurrentUserId() ?: return
        val schedule = paycheckScheduleDao.getScheduleByClientId(scheduleClientId)
            ?: return
        if (schedule.userId != userId) return
        if (!schedule.isActive) return

        paycheckScheduleDao.updateSchedule(
            schedule.copy(
                isActive = false,
                syncStatus = SyncStatus.PENDING
            )
        )
        expenseSyncEnqueuer.enqueue()
    }

    override suspend fun updatePaycheck(paycheck: Paycheck) {
        val userId = authRepository.getCurrentUserId()
            ?: throw IllegalStateException("Sign in to update paychecks")
        val existing = paycheckDao.getPaycheckById(paycheck.id)
            ?: throw IllegalArgumentException("Paycheck not found")
        if (existing.userId != userId) throw IllegalArgumentException("Paycheck not found")

        paycheckDao.updatePaycheck(
            existing.copy(
                amount = paycheck.amount,
                payDate = paycheck.payDateMillis,
                label = paycheck.label,
                syncStatus = SyncStatus.PENDING
            )
        )

        val existingAssignments = paycheckBillDao
            .getAssignmentsForPaycheck(paycheck.id)
            .associateBy { it.billId }
        paycheckBillDao.deleteAssignmentsForPaycheck(paycheck.id)

        insertAssignments(
            userId = userId,
            paycheckId = paycheck.id,
            paycheckClientId = existing.clientId,
            billIds = paycheck.billIds,
            existingAssignments = existingAssignments
        )

        expenseSyncEnqueuer.enqueue()
    }

    /**
     * Writes the bill selection for one paycheck. Reuses the existing
     * assignment rows when [existingAssignments] is supplied so their client
     * ids stay stable across edits.
     */
    private suspend fun insertAssignments(
        userId: String,
        paycheckId: Long,
        paycheckClientId: String,
        billIds: List<Long>,
        existingAssignments: Map<Long, PaycheckBillEntity> = emptyMap()
    ) {
        val now = clock.millis()
        for (billId in billIds.distinct()) {
            val bill = billDao.getBillById(billId) ?: continue
            if (bill.userId != userId) continue
            val previous = existingAssignments[billId]
            paycheckBillDao.insertAssignment(
                PaycheckBillEntity(
                    userId = userId,
                    paycheckId = paycheckId,
                    billId = bill.id,
                    clientId = previous?.clientId
                        ?: UUID.randomUUID().toString(),
                    paycheckClientId = paycheckClientId,
                    billClientId = bill.clientId,
                    createdAt = previous?.createdAt ?: now
                )
            )
        }
    }

    override suspend fun deletePaycheck(id: Long) {
        val userId = authRepository.getCurrentUserId() ?: return
        val existing = paycheckDao.getPaycheckById(id) ?: return
        if (existing.userId != userId) return

        if (existing.scheduleClientId != null) {
            // Keep a tombstone so the deterministic cutoff is not recreated by
            // the next horizon materialization or on another device.
            paycheckDao.updatePaycheck(
                existing.copy(
                    isSkipped = true,
                    syncStatus = SyncStatus.PENDING
                )
            )
        } else {
            pendingPaycheckDeletionDao.insert(
                PendingPaycheckDeletionEntity(clientId = existing.clientId)
            )
            paycheckDao.deletePaycheckById(existing.id)
        }
        paycheckBillDao.deleteAssignmentsForPaycheck(existing.id)

        expenseSyncEnqueuer.enqueue()
    }

    override suspend fun syncPaychecks(userId: String) {
        var failed = false

        // Schedules must exist remotely before their occurrences reference them.
        try {
            syncPaycheckSchedules(userId)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            failed = true
        }

        // Keep a rolling planning window so the user always has upcoming
        // cutoffs to assign bills to.
        try {
            ensureUpcomingPaychecks(userId)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            failed = true
        }

        for (deletion in pendingPaycheckDeletionDao.getPendingDeletions()) {
            try {
                supabasePaycheckRemoteDataSource.deletePaycheckByClientId(
                    deletion.clientId
                )
                pendingPaycheckDeletionDao.deleteByClientId(deletion.clientId)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                failed = true
            }
        }

        for (paycheck in paycheckDao.getUnsyncedPaychecks()) {
            if (paycheck.userId != userId) continue

            try {
                supabasePaycheckRemoteDataSource.upsertPaycheck(
                    SupabasePaycheckDto(
                        clientId = paycheck.clientId,
                        userId = userId,
                        amount = paycheck.amount,
                        payDate = Instant.ofEpochMilli(paycheck.payDate).toString(),
                        label = paycheck.label,
                        createdAt = Instant.ofEpochMilli(paycheck.createdAt).toString(),
                        scheduleClientId = paycheck.scheduleClientId,
                        occurrenceDate = paycheck.occurrenceDate?.let {
                            Instant.ofEpochMilli(it).toString()
                        },
                        isSkipped = paycheck.isSkipped
                    )
                )

                // Replace the remote selection so removed bills disappear too.
                supabasePaycheckRemoteDataSource.deletePaycheckBills(
                    paycheck.clientId
                )

                var assignmentFailed = false
                for (assignment in paycheckBillDao.getUnsyncedForPaycheck(paycheck.id)) {
                    val bill = billDao.getBillById(assignment.billId)
                    if (bill == null || bill.userId != userId) continue
                    try {
                        supabasePaycheckRemoteDataSource.upsertPaycheckBill(
                            SupabasePaycheckBillDto(
                                clientId = assignment.clientId,
                                userId = userId,
                                paycheckClientId = paycheck.clientId,
                                billClientId = bill.clientId,
                                createdAt = Instant
                                    .ofEpochMilli(assignment.createdAt)
                                    .toString()
                            )
                        )
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        assignmentFailed = true
                    }
                }

                if (assignmentFailed) {
                    paycheckDao.updateSyncStatus(
                        paycheckId = paycheck.id,
                        status = SyncStatus.FAILED.name
                    )
                    failed = true
                } else {
                    paycheckDao.updateSyncStatus(
                        paycheckId = paycheck.id,
                        status = SyncStatus.SYNCED.name
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                paycheckDao.updateSyncStatus(
                    paycheckId = paycheck.id,
                    status = SyncStatus.FAILED.name
                )
                failed = true
            }
        }

        if (failed) {
            error("Some paychecks failed to synchronize")
        }
    }

    private suspend fun syncPaycheckSchedules(userId: String) {
        for (schedule in paycheckScheduleDao.getUnsyncedSchedules()) {
            if (schedule.userId != userId) continue

            try {
                supabasePaycheckRemoteDataSource.upsertPaycheckSchedule(
                    SupabasePaycheckScheduleDto(
                        clientId = schedule.clientId,
                        userId = userId,
                        amount = schedule.amount,
                        label = schedule.label,
                        frequency = schedule.frequency,
                        firstDay = schedule.firstDay,
                        secondDay = schedule.secondDay,
                        startDate = Instant.ofEpochMilli(schedule.startDate).toString(),
                        endDate = schedule.endDate?.let {
                            Instant.ofEpochMilli(it).toString()
                        },
                        isActive = schedule.isActive,
                        createdAt = Instant.ofEpochMilli(schedule.createdAt).toString()
                    )
                )
                paycheckScheduleDao.updateSyncStatus(
                    scheduleId = schedule.id,
                    status = SyncStatus.SYNCED.name
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                paycheckScheduleDao.updateSyncStatus(
                    scheduleId = schedule.id,
                    status = SyncStatus.FAILED.name
                )
                throw exception
            }
        }
    }

    private suspend fun ensureUpcomingPaychecks(userId: String) {
        val today = LocalDate.now(clock)
        val horizon = today.plusMonths(HORIZON_MONTHS)

        for (schedule in paycheckScheduleDao.getActiveSchedulesForUser(userId)) {
            // New cutoffs start without bills. The user assigns what is still
            // uncovered, so a bill is never planned on two paychecks.
            materializeOccurrences(
                schedule = schedule,
                from = today,
                to = horizon
            )
        }
    }

    /**
     * Inserts any missing occurrences in [from]..[to]. Deterministic client ids
     * and the unique `(scheduleClientId, occurrenceDate)` index make this safe
     * to run repeatedly.
     *
     * Returns the occurrences created by this call, oldest first, so callers
     * can assign bills to the first cutoff.
     */
    private suspend fun materializeOccurrences(
        schedule: PaycheckScheduleEntity,
        from: LocalDate,
        to: LocalDate
    ): List<PaycheckEntity> {
        if (!schedule.isActive) return emptyList()
        if (from.isAfter(to)) return emptyList()

        val domain = schedule.toDomain()
        val dates = domain.occurrencesBetween(
            from = from,
            to = to,
            zoneId = clock.zone
        )
        if (dates.isEmpty()) return emptyList()

        val now = clock.millis()
        val created = mutableListOf<PaycheckEntity>()

        for (date in dates) {
            val dateMillis = date.atStartOfDay(clock.zone).toInstant().toEpochMilli()
            val occurrence = PaycheckEntity(
                userId = schedule.userId,
                clientId = paycheckOccurrenceClientId(
                    scheduleClientId = schedule.clientId,
                    date = date
                ),
                syncStatus = SyncStatus.PENDING,
                amount = schedule.amount,
                payDate = dateMillis,
                label = schedule.label,
                createdAt = now,
                scheduleClientId = schedule.clientId,
                occurrenceDate = dateMillis
            )
            val inserted = paycheckDao.insertPaycheckIgnoringDuplicate(occurrence)
            if (inserted != -1L) {
                created += occurrence.copy(id = inserted)
            }
        }

        return created
    }

    private companion object {
        /**
         * How far ahead occurrences are materialized. Six months keeps the list
         * useful without generating an unbounded number of future paychecks.
         */
        const val HORIZON_MONTHS = 6L
    }
}
