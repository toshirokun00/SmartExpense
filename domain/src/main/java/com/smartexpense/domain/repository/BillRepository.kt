package com.smartexpense.domain.repository

import com.smartexpense.domain.model.Bill
import com.smartexpense.domain.model.BillPayment
import com.smartexpense.domain.model.PaymentMethod
import kotlinx.coroutines.flow.Flow

interface BillRepository {
    fun getBills(): Flow<List<Bill>>
    fun getPayments(billId: Long): Flow<List<BillPayment>>
    suspend fun getBill(id: Long): Bill?
    suspend fun addBill(bill: Bill): Long
    suspend fun updateBill(bill: Bill)
    suspend fun deleteBill(id: Long)

    /**
     * Records a payment for [id] and advances a recurring bill to its next
     * occurrence. For a variable bill the next estimate is the average of the
     * most recent payments.
     */
    suspend fun payBill(
        id: Long,
        amountPaid: Double,
        paidAtMillis: Long,
        paymentMethod: PaymentMethod
    )

    /**
     * Skips the current occurrence. A recurring bill rolls forward to the next
     * cycle; a one-time bill is marked skipped.
     */
    suspend fun skipBill(id: Long)

    /**
     * Uploads pending bill changes, queued bill deletions and payment history
     * to Supabase so they can be restored on another device.
     */
    suspend fun syncBills(userId: String)
}
