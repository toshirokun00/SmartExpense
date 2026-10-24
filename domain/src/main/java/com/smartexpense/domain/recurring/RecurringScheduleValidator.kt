package com.smartexpense.domain.recurring

import com.smartexpense.domain.model.RecurringExpenseSchedule

/**
 * Validation rules shared by the presentation and data layers.
 *
 * [validate] returns a user-facing message for the first invalid field, or
 * null when the schedule is valid. [requireValid] throws the same message as an
 * [IllegalArgumentException] for repository-level protection.
 */
object RecurringScheduleValidator {

    const val MAX_INTERVAL = 52

    fun validate(schedule: RecurringExpenseSchedule): String? {
        if (schedule.description.isBlank()) {
            return "Please enter a description"
        }

        if (!schedule.amount.isFinite() || schedule.amount <= 0.0) {
            return "Please enter a valid amount"
        }

        if (schedule.categoryId <= 0L) {
            return "Please select a category"
        }

        if (schedule.interval < 1) {
            return "Repeat interval must be at least 1"
        }

        if (schedule.interval > MAX_INTERVAL) {
            return "Repeat interval must be $MAX_INTERVAL or less"
        }

        if (schedule.endDate != null &&
            schedule.endDate < schedule.startDate
        ) {
            return "End date cannot be before start date"
        }

        return null
    }

    fun requireValid(schedule: RecurringExpenseSchedule) {
        validate(schedule)?.let { message ->
            throw IllegalArgumentException(message)
        }
    }
}
