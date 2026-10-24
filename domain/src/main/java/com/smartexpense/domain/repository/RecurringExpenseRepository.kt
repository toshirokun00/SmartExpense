package com.smartexpense.domain.repository

import com.smartexpense.domain.model.RecurringExpenseSchedule
import kotlinx.coroutines.flow.Flow

/**
 * Local, user-scoped access to recurring expense schedules.
 *
 * [processDueSchedules] atomically generates every due occurrence for the
 * currently signed-in user and returns the number of expenses that were
 * created. Implementations are idempotent, so repeated or concurrent calls
 * never create duplicate occurrences.
 */
interface RecurringExpenseRepository {

    fun getSchedules(): Flow<List<RecurringExpenseSchedule>>

    suspend fun getScheduleById(id: Long): RecurringExpenseSchedule?

    suspend fun addSchedule(schedule: RecurringExpenseSchedule)

    suspend fun updateSchedule(schedule: RecurringExpenseSchedule)

    suspend fun setActive(
        id: Long,
        isActive: Boolean
    )

    suspend fun deleteSchedule(id: Long)

    suspend fun processDueSchedules(): Int
}
