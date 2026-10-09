package com.smartexpense.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
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

    @Insert
    suspend fun insertBudget(
        budget: BudgetEntity
    )

    @Update
    suspend fun updateBudget(
        budget: BudgetEntity
    )

    @Delete
    suspend fun deleteBudget(
        budget: BudgetEntity
    )
}