package com.smartexpense.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartexpense.database.entity.PendingBillDeletionEntity

@Dao
interface PendingBillDeletionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(deletion: PendingBillDeletionEntity)

    @Query("SELECT * FROM pending_bill_deletions")
    suspend fun getPendingDeletions(): List<PendingBillDeletionEntity>

    @Query("DELETE FROM pending_bill_deletions WHERE clientId = :clientId")
    suspend fun deleteByClientId(clientId: String)
}
