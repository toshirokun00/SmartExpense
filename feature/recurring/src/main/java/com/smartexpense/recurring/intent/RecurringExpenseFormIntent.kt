package com.smartexpense.recurring.intent

import com.smartexpense.domain.model.RecurrenceFrequency

sealed interface RecurringExpenseFormIntent {

    data class DescriptionChanged(
        val description: String
    ) : RecurringExpenseFormIntent

    data class AmountChanged(
        val amount: String
    ) : RecurringExpenseFormIntent

    data class CategoryChanged(
        val categoryId: Long
    ) : RecurringExpenseFormIntent

    data class FrequencyChanged(
        val frequency: RecurrenceFrequency
    ) : RecurringExpenseFormIntent

    data class IntervalChanged(
        val interval: String
    ) : RecurringExpenseFormIntent

    data class StartDateChanged(
        val date: Long
    ) : RecurringExpenseFormIntent

    data class EndDateChanged(
        val date: Long?
    ) : RecurringExpenseFormIntent

    data class LoadSchedule(
        val id: Long
    ) : RecurringExpenseFormIntent

    data object Save : RecurringExpenseFormIntent
}
