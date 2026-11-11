package com.smartexpense.database

import androidx.room.withTransaction
import com.smartexpense.database.entity.PendingBillDeletionEntity

class BillDeletionTransaction(
    private val database: AppDatabase
) {
    suspend fun deleteBill(
        billId: Long,
        clientId: String
    ) {
        database.withTransaction {
            database.pendingBillDeletionDao().insert(
                PendingBillDeletionEntity(clientId = clientId)
            )

            // Remote payments cascade from the bill, so local history can go too.
            database.billPaymentDao().deletePaymentsForBill(billId)
            database.billDao().deleteBillById(billId)
        }
    }
}
