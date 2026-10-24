package com.smartexpense.recurring.effect

sealed interface RecurringExpenseEffect {

    data object SchedulePaused : RecurringExpenseEffect

    data object ScheduleResumed : RecurringExpenseEffect

    data object ScheduleDeleted : RecurringExpenseEffect

    data class ShowError(
        val message: String
    ) : RecurringExpenseEffect
}

sealed interface RecurringExpenseFormEffect {

    data object Saved : RecurringExpenseFormEffect

    data class ShowError(
        val message: String
    ) : RecurringExpenseFormEffect
}
