package com.smartexpense.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Immutable record of a paid bill occurrence. Payments are kept even after a
 * recurring bill rolls forward, which powers the variable-bill trend and the
 * rolling estimate.
 */
@Entity(
    tableName = "bill_payments",
    indices = [
        Index(value = ["billId"]),
        Index(value = ["userId"]),
        Index(value = ["clientId"], unique = true)
    ]
)
data class BillPaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val userId: String,
    val billId: Long,
    @ColumnInfo(defaultValue = "''")
    val clientId: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "PENDING")
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val billName: String = "",
    val amount: Double,
    val paidAt: Long,
    val paymentMethod: String = "CASH",
    val createdAt: Long
)
