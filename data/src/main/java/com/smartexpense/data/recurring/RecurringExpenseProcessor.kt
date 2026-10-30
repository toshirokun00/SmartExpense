package com.smartexpense.data.recurring

import androidx.room.withTransaction
import com.smartexpense.data.mapper.toDomain
import com.smartexpense.database.AppDatabase
import com.smartexpense.database.entity.ExpenseEntity
import com.smartexpense.database.entity.SyncStatus
import com.smartexpense.domain.recurring.RecurrenceCalculator
import com.smartexpense.domain.recurring.RecurringDateUtils
import com.smartexpense.domain.money.Money
import java.time.LocalDate
import java.time.ZoneId

/**
 * Generates due recurring occurrences and advances the schedule in a single
 * Room transaction.
 *
 * Idempotency has two independent layers:
 *
 *  1. The unique `(recurringScheduleId, occurrenceDate)` index on `expenses`
 *     rejects duplicate occurrences at the database level.
 *  2. `INSERT OR IGNORE` makes retries a no-op instead of an error.
 *
 * Because the expense insert and the `nextOccurrence` advance commit together,
 * process death, retries and concurrent workers can never advance a schedule
 * without writing its occurrence, or write an occurrence twice.
 */
class RecurringExpenseProcessor(
    private val database: AppDatabase
) {

    private val scheduleDao = database.recurringScheduleDao()
    private val expenseDao = database.expenseDao()

    /**
     * @return the number of newly created expenses (duplicates are not
     * counted).
     */
    suspend fun process(
        scheduleId: Long,
        userId: String,
        today: LocalDate,
        zoneId: ZoneId
    ): Int = database.withTransaction {
        val schedule = scheduleDao.getScheduleById(scheduleId)
            ?: return@withTransaction 0

        if (!schedule.isActive || schedule.userId != userId) {
            return@withTransaction 0
        }

        val domain = schedule.toDomain()

        val dueDates = RecurrenceCalculator.dueOccurrences(
            schedule = domain,
            today = today,
            zoneId = zoneId
        )

        if (dueDates.isEmpty()) {
            return@withTransaction 0
        }

        var generated = 0

        for (date in dueDates) {
            val occurrenceMillis = RecurringDateUtils.toEpochMillis(
                date = date,
                zoneId = zoneId
            )

            val insertedId = expenseDao.insertExpenseIgnoringDuplicate(
                ExpenseEntity(
                    amountMinor = Money.toMinorUnits(domain.amount),
                    categoryId = domain.categoryId,
                    description = domain.description,
                    date = occurrenceMillis,
                    syncStatus = SyncStatus.PENDING,
                    clientId = occurrenceClientId(
                        scheduleClientId = domain.clientId,
                        date = date
                    ),
                    userId = userId,
                    recurringScheduleId = domain.id,
                    occurrenceDate = occurrenceMillis
                )
            )

            if (insertedId != -1L) {
                generated++
            }
        }

        val nextOccurrence = RecurrenceCalculator.nextOccurrence(
            schedule = domain,
            current = dueDates.last(),
            zoneId = zoneId
        )

        scheduleDao.advanceNextOccurrence(
            id = domain.id,
            nextOccurrence = RecurringDateUtils.toEpochMillis(
                date = nextOccurrence,
                zoneId = zoneId
            )
        )

        generated
    }

    /**
     * Stable identifier for an occurrence. Combined with the remote
     * `client_id` upsert this also makes remote synchronization idempotent.
     */
    fun occurrenceClientId(
        scheduleClientId: String,
        date: LocalDate
    ): String {
        return "recurring-$scheduleClientId-$date"
    }
}
