package com.smartexpense.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.smartexpense.database.entity.ExpenseEntity
import com.smartexpense.database.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): ExpenseEntity?

    @Insert
    suspend fun insertExpense(expense: ExpenseEntity)

    /**
     * Inserts a generated recurring occurrence unless the unique
     * (recurringScheduleId, occurrenceDate) index already contains it.
     *
     * @return the new row id, or -1 when the occurrence already exists.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExpenseIgnoringDuplicate(
        expense: ExpenseEntity
    ): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("""SELECT * FROM expenses WHERE syncStatus IN ('PENDING', 'FAILED') ORDER BY date ASC""")
    suspend fun getUnsyncedExpenses(): List<ExpenseEntity>

    @Query("UPDATE expenses SET syncStatus = :status WHERE id = :expenseId")
    suspend fun updateSyncStatus(
        expenseId: Long,
        status: String
    )

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpenseById(expenseId: Long)

    @Query("SELECT clientId FROM expenses")
    suspend fun getAllClientIds(): List<String>

    @Query("""
        SELECT * FROM expenses
        WHERE (:query IS NULL OR :query = '' OR LOWER(description) LIKE LOWER('%' || :query || '%'))
          AND (:categoryIdCount = 0 OR categoryId IN (:categoryIds))
          AND (:minAmount IS NULL OR amount >= :minAmount)
          AND (:maxAmount IS NULL OR amount <= :maxAmount)
          AND (:startTime IS NULL OR date >= :startTime)
          AND (:endTime IS NULL OR date < :endTime)
        ORDER BY
          CASE WHEN :sortOrder = 'NEWEST' THEN date END DESC,
          CASE WHEN :sortOrder = 'OLDEST' THEN date END ASC,
          CASE WHEN :sortOrder = 'HIGHEST_AMOUNT' THEN amount END DESC,
          CASE WHEN :sortOrder = 'LOWEST_AMOUNT' THEN amount END ASC,
          id ASC
    """)
    fun searchExpenses(
        query: String?,
        categoryIds: List<Long>,
        categoryIdCount: Int,
        minAmount: Double?,
        maxAmount: Double?,
        startTime: Long?,
        endTime: Long?,
        sortOrder: String
    ): Flow<List<ExpenseEntity>>

}