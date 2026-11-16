package com.smartexpense.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.smartexpense.database.entity.PaycheckEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaycheckDao {
    @Query(
        "SELECT * FROM paychecks WHERE userId = :userId AND isSkipped = 0 " +
            "ORDER BY payDate ASC"
    )
    fun getPaychecksForUser(userId: String): Flow<List<PaycheckEntity>>

    @Query("SELECT * FROM paychecks WHERE id = :id")
    suspend fun getPaycheckById(id: Long): PaycheckEntity?

    @Query("SELECT * FROM paychecks WHERE clientId = :clientId")
    suspend fun getPaycheckByClientId(clientId: String): PaycheckEntity?

    @Query("SELECT clientId FROM paychecks")
    suspend fun getAllClientIds(): List<String>

    @Query("SELECT * FROM paychecks WHERE syncStatus IN ('PENDING', 'FAILED')")
    suspend fun getUnsyncedPaychecks(): List<PaycheckEntity>

    @Query("UPDATE paychecks SET syncStatus = :status WHERE id = :paycheckId")
    suspend fun updateSyncStatus(paycheckId: Long, status: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaycheck(paycheck: PaycheckEntity): Long

    /**
     * Used while materializing recurring occurrences. A duplicate
     * `(scheduleClientId, occurrenceDate)` or `clientId` is ignored so retries
     * are idempotent.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPaycheckIgnoringDuplicate(paycheck: PaycheckEntity): Long

    @Update
    suspend fun updatePaycheck(paycheck: PaycheckEntity)

    @Delete
    suspend fun deletePaycheck(paycheck: PaycheckEntity)

    @Query("DELETE FROM paychecks WHERE id = :paycheckId")
    suspend fun deletePaycheckById(paycheckId: Long)
}
