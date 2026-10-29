package com.smartexpense.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_budget_deletions")
data class PendingBudgetDeletionEntity(
    @PrimaryKey
    val clientId: String
)
