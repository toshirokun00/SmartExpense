package com.smartexpense.paycheck.intent

sealed interface PaychecksIntent {
    data object PreviousMonth : PaychecksIntent
    data object NextMonth : PaychecksIntent
    data object CurrentMonth : PaychecksIntent
    data class SelectMonth(val year: Int, val month: Int) : PaychecksIntent
}
