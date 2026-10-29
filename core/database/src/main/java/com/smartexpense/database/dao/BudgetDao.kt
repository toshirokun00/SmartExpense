package com.smartexpense.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.smartexpense.database.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("""SELECT * FROM budgets ORDER BY startDate DESC """)
    fun getBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getBudgetById(
        id: Long
    ): BudgetEntity?

    @Query("SELECT * FROM budgets")
    suspend fun getBudgetsOnce(): List<BudgetEntity>

    @Insert
    suspend fun insertBudget(
        budget: BudgetEntity
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBudgetIgnoringDuplicate(budget: BudgetEntity): Long

    @Query("SELECT * FROM budgets WHERE syncStatus IN ('PENDING', 'FAILED')")
    suspend fun getUnsyncedBudgets(): List<BudgetEntity>

    @Query("UPDATE budgets SET syncStatus = :status WHERE id = :budgetId")
    suspend fun updateSyncStatus(budgetId: Long, status: String)

    @Update
    suspend fun updateBudget(
        budget: BudgetEntity
    )

    @Delete
    suspend fun deleteBudget(
        budget: BudgetEntity
    )

    @Query("DELETE FROM budgets WHERE id = :budgetId")
    suspend fun deleteBudgetById(budgetId: Long)
}