package com.smartexpense.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartexpense.database.entity.PendingPaycheckDeletionEntity

@Dao
interface PendingPaycheckDeletionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(deletion: PendingPaycheckDeletionEntity)

    @Query("SELECT * FROM pending_paycheck_deletions")
    suspend fun getPendingDeletions(): List<PendingPaycheckDeletionEntity>

    @Query("DELETE FROM pending_paycheck_deletions WHERE clientId = :clientId")
    suspend fun deleteByClientId(clientId: String)
}
