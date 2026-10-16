package com.smartexpense.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartexpense.database.entity.PendingExpenseDeletionEntity

@Dao
interface PendingExpenseDeletionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(deletion: PendingExpenseDeletionEntity)

    @Query("SELECT * FROM pending_expense_deletions")
    suspend fun getPendingDeletions(): List<PendingExpenseDeletionEntity>

    @Query("DELETE FROM pending_expense_deletions WHERE clientId = :clientId")
    suspend fun deleteByClientId(clientId: String)
}