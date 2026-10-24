package com.smartexpense.recurring.intent

sealed interface RecurringExpenseIntent {

    data class Pause(
        val id: Long
    ) : RecurringExpenseIntent

    data class Resume(
        val id: Long
    ) : RecurringExpenseIntent

    data class RequestDelete(
        val id: Long
    ) : RecurringExpenseIntent

    data object ConfirmDelete : RecurringExpenseIntent

    data object CancelDelete : RecurringExpenseIntent
}
