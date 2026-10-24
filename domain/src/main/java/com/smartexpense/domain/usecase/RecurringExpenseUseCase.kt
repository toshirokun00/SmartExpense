package com.smartexpense.domain.usecase

import com.smartexpense.domain.model.RecurringExpenseSchedule
import com.smartexpense.domain.repository.RecurringExpenseRepository
import kotlinx.coroutines.flow.Flow

class RecurringExpenseUseCase(
    private val repository: RecurringExpenseRepository
) {

    fun getSchedules(): Flow<List<RecurringExpenseSchedule>> {
        return repository.getSchedules()
    }

    suspend fun getScheduleById(
        id: Long
    ): RecurringExpenseSchedule? {
        return repository.getScheduleById(id)
    }

    suspend fun addSchedule(
        schedule: RecurringExpenseSchedule
    ) {
        repository.addSchedule(schedule)
    }

    suspend fun updateSchedule(
        schedule: RecurringExpenseSchedule
    ) {
        repository.updateSchedule(schedule)
    }

    suspend fun pauseSchedule(
        id: Long
    ) {
        repository.setActive(
            id = id,
            isActive = false
        )
    }

    suspend fun resumeSchedule(
        id: Long
    ) {
        repository.setActive(
            id = id,
            isActive = true
        )
    }

    suspend fun deleteSchedule(
        id: Long
    ) {
        repository.deleteSchedule(id)
    }
}
