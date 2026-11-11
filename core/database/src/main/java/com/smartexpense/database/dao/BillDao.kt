package com.smartexpense.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.smartexpense.database.entity.BillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Query("SELECT * FROM bills WHERE userId = :userId ORDER BY isPaid ASC, dueDate ASC")
    fun getBillsForUser(userId: String): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getBillById(id: Long): BillEntity?

    @Query("SELECT * FROM bills WHERE clientId = :clientId")
    suspend fun getBillByClientId(clientId: String): BillEntity?

    @Query("SELECT clientId FROM bills")
    suspend fun getAllClientIds(): List<String>

    @Query("SELECT * FROM bills WHERE syncStatus IN ('PENDING', 'FAILED')")
    suspend fun getUnsyncedBills(): List<BillEntity>

    @Query("UPDATE bills SET syncStatus = :status WHERE id = :billId")
    suspend fun updateSyncStatus(billId: Long, status: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: BillEntity): Long

    @Update
    suspend fun updateBill(bill: BillEntity)

    @Delete
    suspend fun deleteBill(bill: BillEntity)

    @Query("DELETE FROM bills WHERE id = :billId")
    suspend fun deleteBillById(billId: Long)
}
