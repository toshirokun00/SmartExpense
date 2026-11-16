package com.smartexpense.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Links a bill to the paycheck that is expected to cover it.
 *
 * Remote rows reference the paycheck and bill by their stable client ids so the
 * relation survives a restore on another device.
 */
@Entity(
    tableName = "paycheck_bills",
    indices = [
        Index(value = ["paycheckId"]),
        Index(value = ["billId"]),
        Index(value = ["clientId"], unique = true),
        Index(value = ["paycheckClientId", "billClientId"], unique = true)
    ]
)
data class PaycheckBillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val userId: String,
    val paycheckId: Long,
    val billId: Long,
    val clientId: String = UUID.randomUUID().toString(),
    val paycheckClientId: String,
    val billClientId: String,
    val createdAt: Long
)
