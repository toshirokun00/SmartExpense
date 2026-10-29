package com.smartexpense.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartexpense.database.entity.PendingBudgetDeletionEntity

@Dao
interface PendingBudgetDeletionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(deletion: PendingBudgetDeletionEntity)

    @Query("SELECT * FROM pending_budget_deletions")
    suspend fun getPendingDeletions(): List<PendingBudgetDeletionEntity>

    @Query("DELETE FROM pending_budget_deletions WHERE clientId = :clientId")
    suspend fun deleteByClientId(clientId: String)
}
