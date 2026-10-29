package com.smartexpense.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["userId"])]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val categoryId: Long,
    val amount: Double,
    val startDate: Long,
    val endDate: Long,
    val clientId: String = UUID.randomUUID().toString(),
    val userId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)