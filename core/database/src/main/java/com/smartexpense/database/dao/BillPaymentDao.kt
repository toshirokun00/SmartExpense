package com.smartexpense.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.smartexpense.database.entity.BillPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BillPaymentDao {

    @Insert
    suspend fun insertPayment(payment: BillPaymentEntity): Long

    @Query("SELECT * FROM bill_payments WHERE billId = :billId ORDER BY paidAt DESC")
    fun getPaymentsForBill(billId: Long): Flow<List<BillPaymentEntity>>

    /** All payments for a user, newest first, used by the payment history screen. */
    @Query("SELECT * FROM bill_payments WHERE userId = :userId ORDER BY paidAt DESC")
    fun getPaymentsForUser(userId: String): Flow<List<BillPaymentEntity>>

    @Query(
        "SELECT * FROM bill_payments WHERE billId = :billId " +
            "ORDER BY paidAt DESC LIMIT :limit"
    )
    suspend fun getRecentPaymentsForBill(
        billId: Long,
        limit: Int
    ): List<BillPaymentEntity>

    @Query("SELECT clientId FROM bill_payments")
    suspend fun getAllClientIds(): List<String>

    @Query("SELECT * FROM bill_payments WHERE syncStatus IN ('PENDING', 'FAILED')")
    suspend fun getUnsyncedPayments(): List<BillPaymentEntity>

    @Query("UPDATE bill_payments SET syncStatus = :status WHERE id = :paymentId")
    suspend fun updateSyncStatus(paymentId: Long, status: String)

    @Query("DELETE FROM bill_payments WHERE billId = :billId")
    suspend fun deletePaymentsForBill(billId: Long)
}
