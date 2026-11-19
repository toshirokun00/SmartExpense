package com.smartexpense.data.mapper

import com.smartexpense.database.entity.PaycheckBillEntity
import com.smartexpense.database.entity.PaycheckEntity
import com.smartexpense.domain.model.Paycheck

fun PaycheckEntity.toDomain(billIds: List<Long> = emptyList()): Paycheck = Paycheck(
    id = id,
    amount = amount,
    payDateMillis = payDate,
    label = label,
    billIds = billIds,
    createdAt = createdAt,
    scheduleClientId = scheduleClientId,
    occurrenceDateMillis = occurrenceDate,
    isSkipped = isSkipped
)

fun Paycheck.toEntity(userId: String): PaycheckEntity = PaycheckEntity(
    id = id,
    userId = userId,
    amount = amount,
    payDate = payDateMillis,
    label = label,
    createdAt = createdAt,
    scheduleClientId = scheduleClientId,
    occurrenceDate = occurrenceDateMillis,
    isSkipped = isSkipped
)

fun PaycheckBillEntity.toDomainBillId(): Long = billId
