package com.smartexpense.data.mapper

import com.smartexpense.database.entity.PaycheckScheduleEntity
import com.smartexpense.domain.model.PaycheckFrequency
import com.smartexpense.domain.model.PaycheckSchedule

fun PaycheckScheduleEntity.toDomain(): PaycheckSchedule = PaycheckSchedule(
    id = id,
    clientId = clientId,
    amount = amount,
    label = label,
    frequency = PaycheckFrequency.valueOf(frequency),
    firstDay = firstDay,
    secondDay = secondDay,
    startDateMillis = startDate,
    endDateMillis = endDate,
    isActive = isActive,
    createdAt = createdAt
)

fun PaycheckSchedule.toEntity(userId: String): PaycheckScheduleEntity =
    PaycheckScheduleEntity(
        id = id,
        userId = userId,
        clientId = clientId,
        amount = amount,
        label = label,
        frequency = frequency.name,
        firstDay = firstDay,
        secondDay = secondDay,
        startDate = startDateMillis,
        endDate = endDateMillis,
        isActive = isActive,
        createdAt = createdAt
    )
