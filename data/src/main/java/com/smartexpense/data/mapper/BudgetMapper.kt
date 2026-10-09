package com.smartexpense.data.mapper

import com.smartexpense.database.entity.BudgetEntity
import com.smartexpense.domain.model.Budget

fun BudgetEntity.toDomain(): Budget {
    return Budget(
        id = id,
        categoryId = categoryId,
        amount = amount,
        startDate = startDate,
        endDate = endDate
    )
}

fun Budget.toEntity(): BudgetEntity {
    return BudgetEntity(
        id = id,
        categoryId = categoryId,
        amount = amount,
        startDate = startDate,
        endDate = endDate
    )
}