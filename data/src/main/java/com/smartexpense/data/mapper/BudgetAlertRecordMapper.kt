package com.smartexpense.data.mapper

import com.smartexpense.database.entity.BudgetAlertRecordEntity
import com.smartexpense.domain.model.BudgetAlertRecord

fun BudgetAlertRecordEntity.toDomain(): BudgetAlertRecord {
    return BudgetAlertRecord(
        budgetId = budgetId,
        periodStart = periodStart,
        periodEnd = periodEnd,
        threshold = threshold
    )
}

fun BudgetAlertRecord.toEntity(): BudgetAlertRecordEntity {
    return BudgetAlertRecordEntity(
        budgetId = budgetId,
        periodStart = periodStart,
        periodEnd = periodEnd,
        threshold = threshold
    )
}
