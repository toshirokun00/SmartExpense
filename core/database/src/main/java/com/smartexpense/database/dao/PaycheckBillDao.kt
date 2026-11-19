package com.smartexpense.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartexpense.database.entity.PaycheckBillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaycheckBillDao {
    @Query("SELECT * FROM paycheck_bills WHERE userId = :userId ORDER BY id ASC")
    fun getAssignmentsForUser(userId: String): Flow<List<PaycheckBillEntity>>

    @Query("SELECT * FROM paycheck_bills WHERE paycheckId = :paycheckId ORDER BY id ASC")
    suspend fun getAssignmentsForPaycheck(paycheckId: Long): List<PaycheckBillEntity>

    @Query("SELECT clientId FROM paycheck_bills")
    suspend fun getAllClientIds(): List<String>

    @Query("SELECT * FROM paycheck_bills WHERE paycheckId = :paycheckId")
    suspend fun getUnsyncedForPaycheck(paycheckId: Long): List<PaycheckBillEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAssignment(assignment: PaycheckBillEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAssignments(assignments: List<PaycheckBillEntity>)

    @Query("DELETE FROM paycheck_bills WHERE paycheckId = :paycheckId")
    suspend fun deleteAssignmentsForPaycheck(paycheckId: Long)

    @Query("DELETE FROM paycheck_bills WHERE billId = :billId")
    suspend fun deleteAssignmentsForBill(billId: Long)
}
