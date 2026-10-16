package com.smartexpense.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val amount: Double,
    val categoryId: Long,
    val description: String,
    val date: Long,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val clientId: String = UUID.randomUUID().toString()
)

enum class SyncStatus {
    PENDING,
    SYNCED,
    FAILED
}