package com.smartexpense.data.repositoryimpl.mapper

import com.smartexpense.database.entity.ExpenseEntity
import com.smartexpense.domain.model.Expense

fun ExpenseEntity.toDomain() : Expense {
    return Expense(
        id = id,
        amount = amount,
        categoryId = categoryId,
        description = description,
        date = date
    )
}

fun Expense.toEntity() : ExpenseEntity {
    return ExpenseEntity(
    id = id,
    amount = amount,
    categoryId = categoryId,
    description = description,
    date = date
    )
}