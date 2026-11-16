package com.smartexpense.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Queues a paycheck deletion so it can be applied remotely on the next sync. */
@Entity(tableName = "pending_paycheck_deletions")
data class PendingPaycheckDeletionEntity(
    @PrimaryKey
    val clientId: String
)
