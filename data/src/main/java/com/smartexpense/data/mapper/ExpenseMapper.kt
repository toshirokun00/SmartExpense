package com.smartexpense.data.repositoryimpl.mapper

import com.smartexpense.database.entity.ExpenseEntity
import com.smartexpense.database.entity.SyncStatus
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.model.ExpenseSyncStatus

fun ExpenseEntity.toDomain(): Expense = Expense.fromMinorUnits(
    id = id,
    amountMinor = amountMinor,
    categoryId = categoryId,
    description = description,
    date = date,
    merchant = merchant,
    syncStatus = syncStatus.toDomain()
)

fun Expense.toEntity(): ExpenseEntity = ExpenseEntity(
    id = id,
    amountMinor = amountMinor,
    categoryId = categoryId,
    description = description,
    date = date,
    merchant = merchant,
    syncStatus = syncStatus.toEntity()
)

fun SyncStatus.toDomain(): ExpenseSyncStatus = when (this) {
    SyncStatus.PENDING -> ExpenseSyncStatus.PENDING
    SyncStatus.SYNCED -> ExpenseSyncStatus.SYNCED
    SyncStatus.FAILED -> ExpenseSyncStatus.FAILED
}

fun ExpenseSyncStatus.toEntity(): SyncStatus = when (this) {
    ExpenseSyncStatus.PENDING -> SyncStatus.PENDING
    ExpenseSyncStatus.SYNCED -> SyncStatus.SYNCED
    ExpenseSyncStatus.FAILED -> SyncStatus.FAILED
}
