package com.smartexpense.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * A recurring payroll rule. The rule itself does not show up as income; it
 * generates normal [PaycheckEntity] occurrences through the repository.
 */
@Entity(
    tableName = "paycheck_schedules",
    indices = [
        Index(value = ["clientId"], unique = true),
        Index(value = ["userId", "isActive"])
    ]
)
data class PaycheckScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val userId: String,
    val clientId: String = UUID.randomUUID().toString(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val amount: Double,
    val label: String = "",
    /** One of [com.smartexpense.domain.model.PaycheckFrequency]. */
    val frequency: String,
    val firstDay: Int,
    val secondDay: Int? = null,
    val startDate: Long,
    val endDate: Long? = null,
    val isActive: Boolean = true,
    val createdAt: Long
)
