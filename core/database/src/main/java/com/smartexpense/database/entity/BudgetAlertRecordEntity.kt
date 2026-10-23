package com.smartexpense.database.entity

import androidx.room.Entity

/**
 * Delivery history for budget alerts. The composite key (budget + period +
 * threshold) makes duplicate deliveries for the same budget period impossible.
 */
@Entity(
    tableName = "budget_alert_records",
    primaryKeys = ["budgetId", "periodStart", "periodEnd", "threshold"]
)
data class BudgetAlertRecordEntity(
    val budgetId: Long,
    val periodStart: Long,
    val periodEnd: Long,
    val threshold: Int
)
