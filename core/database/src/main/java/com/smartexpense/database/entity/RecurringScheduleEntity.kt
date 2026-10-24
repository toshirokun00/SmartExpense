package com.smartexpense.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Persistent recurring expense schedule.
 *
 * [frequency] stores [com.smartexpense.domain.model.RecurrenceFrequency.name]
 * as a string so the database module stays independent of the domain module.
 * All dates are epoch milliseconds at the start of the day.
 */
@Entity(
    tableName = "recurring_schedules",
    indices = [
        Index(
            value = [
                "userId",
                "isActive",
                "nextOccurrence"
            ]
        ),
        Index(
            value = ["clientId"],
            unique = true
        )
    ]
)
data class RecurringScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val clientId: String = UUID.randomUUID().toString(),
    val userId: String,
    val description: String,
    val amount: Double,
    val categoryId: Long,
    val frequency: String,
    val interval: Int = 1,
    val startDate: Long,
    val endDate: Long? = null,
    val nextOccurrence: Long,
    val isActive: Boolean = true
)
