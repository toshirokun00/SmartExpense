package com.smartexpense.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.smartexpense.database.entity.RecurringScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringScheduleDao {

    @Query(
        """
        SELECT * FROM recurring_schedules
        WHERE userId = :userId
        ORDER BY isActive DESC, nextOccurrence ASC, id ASC
        """
    )
    fun getSchedulesForUser(
        userId: String
    ): Flow<List<RecurringScheduleEntity>>

    @Query("SELECT * FROM recurring_schedules WHERE id = :id")
    suspend fun getScheduleById(
        id: Long
    ): RecurringScheduleEntity?

    @Query(
        """
        SELECT * FROM recurring_schedules
        WHERE userId = :userId
          AND isActive = 1
          AND nextOccurrence <= :nowMillis
        ORDER BY nextOccurrence ASC, id ASC
        """
    )
    suspend fun getDueSchedules(
        userId: String,
        nowMillis: Long
    ): List<RecurringScheduleEntity>

    /**
     * Inserts a schedule unless its unique [clientId] already exists.
     *
     * @return the new row id, or -1 when the schedule was already stored.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertScheduleIgnoringDuplicate(
        schedule: RecurringScheduleEntity
    ): Long

    @Update
    suspend fun updateSchedule(
        schedule: RecurringScheduleEntity
    )

    @Delete
    suspend fun deleteSchedule(
        schedule: RecurringScheduleEntity
    )

    /**
     * Advances [nextOccurrence] only forward, so concurrent or stale writers
     * can never move a schedule backwards.
     */
    @Query(
        """
        UPDATE recurring_schedules
        SET nextOccurrence = :nextOccurrence
        WHERE id = :id
          AND nextOccurrence < :nextOccurrence
        """
    )
    suspend fun advanceNextOccurrence(
        id: Long,
        nextOccurrence: Long
    )

    @Query("UPDATE recurring_schedules SET isActive = :isActive WHERE id = :id")
    suspend fun setActive(
        id: Long,
        isActive: Boolean
    )

    @Query("SELECT MAX(occurrenceDate) FROM expenses WHERE recurringScheduleId = :scheduleId")
    suspend fun getLatestOccurrenceDate(
        scheduleId: Long
    ): Long?
}
