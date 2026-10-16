package com.smartexpense.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_expense_deletions")
data class PendingExpenseDeletionEntity(
    @PrimaryKey
    val clientId: String,
    val remoteId: Long? = null
)