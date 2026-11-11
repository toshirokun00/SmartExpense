package com.smartexpense.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_bill_deletions")
data class PendingBillDeletionEntity(
    @PrimaryKey
    val clientId: String
)
