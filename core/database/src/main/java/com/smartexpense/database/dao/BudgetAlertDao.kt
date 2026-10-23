package com.smartexpense.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartexpense.database.entity.BudgetAlertRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetAlertDao {

    @Query("SELECT * FROM budget_alert_records")
    fun getRecords(): Flow<List<BudgetAlertRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecords(records: List<BudgetAlertRecordEntity>)

    @Query(
        """
        DELETE FROM budget_alert_records
        WHERE budgetId = :budgetId
          AND periodStart = :periodStart
          AND periodEnd = :periodEnd
          AND threshold = :threshold
        """
    )
    suspend fun deleteRecord(
        budgetId: Long,
        periodStart: Long,
        periodEnd: Long,
        threshold: Int
    )
}
