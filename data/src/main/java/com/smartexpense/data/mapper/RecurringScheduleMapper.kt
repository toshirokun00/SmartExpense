package com.smartexpense.data.mapper

import com.smartexpense.database.entity.RecurringScheduleEntity
import com.smartexpense.domain.model.RecurrenceFrequency
import com.smartexpense.domain.model.RecurringExpenseSchedule

fun RecurringScheduleEntity.toDomain(): RecurringExpenseSchedule {
    return RecurringExpenseSchedule(
        id = id,
        clientId = clientId,
        userId = userId,
        description = description,
        amount = amount,
        categoryId = categoryId,
        frequency = RecurrenceFrequency.valueOf(frequency),
        interval = interval,
        startDate = startDate,
        endDate = endDate,
        nextOccurrence = nextOccurrence,
        isActive = isActive
    )
}

fun RecurringExpenseSchedule.toEntity(): RecurringScheduleEntity {
    return RecurringScheduleEntity(
        id = id,
        clientId = clientId,
        userId = userId,
        description = description,
        amount = amount,
        categoryId = categoryId,
        frequency = frequency.name,
        interval = interval,
        startDate = startDate,
        endDate = endDate,
        nextOccurrence = nextOccurrence,
        isActive = isActive
    )
}
