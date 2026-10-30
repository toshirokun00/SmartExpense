package com.smartexpense.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * A normal expense row.
 *
 * Recurring occurrences additionally carry [recurringScheduleId] and
 * [occurrenceDate]. The unique composite index enforces occurrence uniqueness at
 * the database level; manual expenses have both columns set to null, and SQLite
 * treats null values as distinct, so they are unaffected.
 *
 * [userId] is set for locally generated recurring expenses and for rows pulled
 * from Supabase. It lets the sync pipeline avoid uploading one user's pending
 * rows under another account.
 */
@Entity(
    tableName = "expenses",
    indices = [
        Index(
            value = [
                "recurringScheduleId",
                "occurrenceDate"
            ],
            unique = true
        ),
        Index(value = ["userId"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val amountMinor: Long,
    val categoryId: Long,
    val description: String,
    val date: Long,
    val merchant: String = "",
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val clientId: String = UUID.randomUUID().toString(),
    val userId: String? = null,
    val recurringScheduleId: Long? = null,
    val occurrenceDate: Long? = null
)

enum class SyncStatus {
    PENDING,
    SYNCED,
    FAILED
}
