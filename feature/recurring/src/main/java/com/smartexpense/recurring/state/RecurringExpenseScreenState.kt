package com.smartexpense.recurring.state

data class RecurringExpenseScreenState(
    val showDeleteConfirmation: Boolean = false,
    val scheduleToDeleteId: Long? = null
)
