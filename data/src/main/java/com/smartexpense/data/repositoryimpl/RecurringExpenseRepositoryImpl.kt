package com.smartexpense.data.repositoryimpl

import com.smartexpense.data.mapper.toDomain
import com.smartexpense.data.mapper.toEntity
import com.smartexpense.data.recurring.RecurringExpenseProcessor
import com.smartexpense.data.sync.ExpenseSyncEnqueuer
import com.smartexpense.data.sync.RecurringExpenseScheduler
import com.smartexpense.database.dao.RecurringScheduleDao
import com.smartexpense.domain.model.RecurringExpenseSchedule
import com.smartexpense.domain.recurring.RecurrenceCalculator
import com.smartexpense.domain.recurring.RecurringDateUtils
import com.smartexpense.domain.recurring.RecurringScheduleValidator
import com.smartexpense.domain.repository.AuthRepository
import com.smartexpense.domain.repository.RecurringExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/**
 * Room-backed recurring expense repository.
 *
 * Schedules are local-only (the same convention the app already uses for
 * budgets). They are scoped to the currently signed-in user, and generated
 * expenses go through the existing expense table and sync pipeline.
 */
class RecurringExpenseRepositoryImpl(
    private val scheduleDao: RecurringScheduleDao,
    private val authRepository: AuthRepository,
    private val processor: RecurringExpenseProcessor,
    private val expenseSyncEnqueuer: ExpenseSyncEnqueuer,
    private val scheduler: RecurringExpenseScheduler,
    private val clock: Clock
) : RecurringExpenseRepository {

    override fun getSchedules(): Flow<List<RecurringExpenseSchedule>> {
        return flow {
            val userId = authRepository.getCurrentUserId()

            if (userId == null) {
                emit(emptyList())
                return@flow
            }

            emitAll(
                scheduleDao
                    .getSchedulesForUser(userId)
                    .map { schedules ->
                        schedules.map { it.toDomain() }
                    }
            )
        }
    }

    override suspend fun getScheduleById(
        id: Long
    ): RecurringExpenseSchedule? {
        val userId = authRepository.getCurrentUserId()
            ?: return null

        val schedule = scheduleDao.getScheduleById(id)
            ?: return null

        return if (schedule.userId == userId) {
            schedule.toDomain()
        } else {
            null
        }
    }

    override suspend fun addSchedule(
        schedule: RecurringExpenseSchedule
    ) {
        val userId = requireUserId()

        RecurringScheduleValidator.requireValid(schedule)

        val startDate = normalize(schedule.startDate)
        val endDate = schedule.endDate?.let(::normalize)

        val entity = schedule
            .copy(
                userId = userId,
                startDate = startDate,
                endDate = endDate,
                nextOccurrence = startDate,
                isActive = true
            )
            .toEntity()

        // The unique clientId index makes a retried save idempotent.
        scheduleDao.insertScheduleIgnoringDuplicate(entity)

        scheduler.enqueueImmediate()
    }

    override suspend fun updateSchedule(
        schedule: RecurringExpenseSchedule
    ) {
        val userId = requireUserId()

        val existing = scheduleDao.getScheduleById(schedule.id)
            ?: throw IllegalArgumentException("Recurring expense not found")

        if (existing.userId != userId) {
            throw IllegalArgumentException("Recurring expense not found")
        }

        RecurringScheduleValidator.requireValid(schedule)

        val startDate = normalize(schedule.startDate)
        val endDate = schedule.endDate?.let(::normalize)

        val updated = existing.toDomain().copy(
            description = schedule.description,
            amount = schedule.amount,
            categoryId = schedule.categoryId,
            frequency = schedule.frequency,
            interval = schedule.interval,
            startDate = startDate,
            endDate = endDate
        )

        val latestOccurrence = scheduleDao.getLatestOccurrenceDate(existing.id)

        val nextOccurrence = if (latestOccurrence == null) {
            startDate
        } else {
            val nextDate = RecurrenceCalculator.nextOccurrence(
                schedule = updated,
                current = RecurringDateUtils.toLocalDate(
                    epochMillis = latestOccurrence,
                    zoneId = zone
                ),
                zoneId = zone
            )

            val startLocalDate = RecurringDateUtils.toLocalDate(
                epochMillis = startDate,
                zoneId = zone
            )

            if (nextDate.isBefore(startLocalDate)) {
                startDate
            } else {
                RecurringDateUtils.toEpochMillis(
                    date = nextDate,
                    zoneId = zone
                )
            }
        }

        scheduleDao.updateSchedule(
            updated.copy(nextOccurrence = nextOccurrence).toEntity()
        )

        scheduler.enqueueImmediate()
    }

    override suspend fun setActive(
        id: Long,
        isActive: Boolean
    ) {
        val userId = requireUserId()

        val existing = scheduleDao.getScheduleById(id)
            ?: throw IllegalArgumentException("Recurring expense not found")

        if (existing.userId != userId) {
            throw IllegalArgumentException("Recurring expense not found")
        }

        if (existing.isActive == isActive) {
            return
        }

        if (!isActive) {
            scheduleDao.setActive(
                id = id,
                isActive = false
            )
            return
        }

        // Resuming intentionally skips occurrences that became due while the
        // schedule was paused: the next occurrence is the first one on or after
        // today. Active schedules still catch up missed occurrences.
        val nextOccurrence = RecurrenceCalculator.firstOnOrAfter(
            schedule = existing.toDomain(),
            date = LocalDate.now(clock),
            zoneId = zone
        )

        scheduleDao.updateSchedule(
            existing.copy(
                isActive = true,
                nextOccurrence = RecurringDateUtils.toEpochMillis(
                    date = nextOccurrence,
                    zoneId = zone
                )
            )
        )

        scheduler.enqueueImmediate()
    }

    override suspend fun deleteSchedule(
        id: Long
    ) {
        val userId = requireUserId()

        val existing = scheduleDao.getScheduleById(id)
            ?: return

        if (existing.userId != userId) {
            return
        }

        scheduleDao.deleteSchedule(existing)
    }

    override suspend fun processDueSchedules(): Int {
        val userId = authRepository.getCurrentUserId()
            ?: return 0

        val today = LocalDate.now(clock)
        val nowMillis = RecurringDateUtils.endOfDayMillis(
            date = today,
            zoneId = zone
        )

        val dueSchedules = scheduleDao.getDueSchedules(
            userId = userId,
            nowMillis = nowMillis
        )

        if (dueSchedules.isEmpty()) {
            return 0
        }

        var generated = 0

        try {
            for (schedule in dueSchedules) {
                generated += processor.process(
                    scheduleId = schedule.id,
                    userId = userId,
                    today = today,
                    zoneId = zone
                )
            }
        } finally {
            if (generated > 0) {
                expenseSyncEnqueuer.enqueue()
            }
        }

        return generated
    }

    private suspend fun requireUserId(): String {
        return authRepository.getCurrentUserId()
            ?: throw IllegalStateException(
                "Sign in to manage recurring expenses"
            )
    }

    private fun normalize(
        epochMillis: Long
    ): Long {
        return RecurringDateUtils.toEpochMillis(
            date = RecurringDateUtils.toLocalDate(
                epochMillis = epochMillis,
                zoneId = zone
            ),
            zoneId = zone
        )
    }

    private val zone: ZoneId
        get() = clock.zone
}
