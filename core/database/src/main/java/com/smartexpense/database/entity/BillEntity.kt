package com.smartexpense.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "bills",
    indices = [
        Index(value = ["userId", "isPaid", "dueDate"]),
        Index(value = ["clientId"], unique = true)
    ]
)
data class BillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val userId: String,
    @ColumnInfo(defaultValue = "''")
    val clientId: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "PENDING")
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val name: String,
    val amount: Double,
    val dueDate: Long,
    val isPaid: Boolean = false,
    val notes: String = "",
    @ColumnInfo(defaultValue = "NONE")
    val frequency: String = "NONE",
    @ColumnInfo(defaultValue = "1")
    val repeatInterval: Int = 1,
    @ColumnInfo(defaultValue = "STATIC")
    val amountType: String = "STATIC",
    @ColumnInfo(defaultValue = "0")
    val amountEstimated: Boolean = false,
    val categoryId: Long? = null,
    @ColumnInfo(defaultValue = "")
    val payee: String = "",
    @ColumnInfo(defaultValue = "CASH")
    val paymentMethod: String = "CASH",
    @ColumnInfo(defaultValue = "0")
    val autopay: Boolean = false,
    @ColumnInfo(defaultValue = "3")
    val reminderDaysBefore: Int = 3,
    @ColumnInfo(defaultValue = "540")
    val reminderTimeMinutes: Int = 540,
    val paidAt: Long? = null,
    @ColumnInfo(defaultValue = "0")
    val skipped: Boolean = false,
    @ColumnInfo(defaultValue = "1")
    val isActive: Boolean = true
)
