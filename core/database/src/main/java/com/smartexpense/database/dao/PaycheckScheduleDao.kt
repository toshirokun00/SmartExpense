package com.smartexpense.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.smartexpense.database.entity.PaycheckScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaycheckScheduleDao {

    @Query("SELECT * FROM paycheck_schedules WHERE userId = :userId ORDER BY createdAt ASC")
    fun getSchedulesForUser(userId: String): Flow<List<PaycheckScheduleEntity>>

    @Query(
        "SELECT * FROM paycheck_schedules " +
            "WHERE userId = :userId AND isActive = 1 ORDER BY createdAt ASC"
    )
    suspend fun getActiveSchedulesForUser(userId: String): List<PaycheckScheduleEntity>

    @Query("SELECT * FROM paycheck_schedules WHERE id = :id")
    suspend fun getScheduleById(id: Long): PaycheckScheduleEntity?

    @Query("SELECT * FROM paycheck_schedules WHERE clientId = :clientId")
    suspend fun getScheduleByClientId(clientId: String): PaycheckScheduleEntity?

    @Query("SELECT clientId FROM paycheck_schedules")
    suspend fun getAllClientIds(): List<String>

    @Query("SELECT * FROM paycheck_schedules WHERE syncStatus IN ('PENDING', 'FAILED')")
    suspend fun getUnsyncedSchedules(): List<PaycheckScheduleEntity>

    @Query("UPDATE paycheck_schedules SET syncStatus = :status WHERE id = :scheduleId")
    suspend fun updateSyncStatus(scheduleId: Long, status: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertScheduleIgnoringDuplicate(schedule: PaycheckScheduleEntity): Long

    @Update
    suspend fun updateSchedule(schedule: PaycheckScheduleEntity)
}
