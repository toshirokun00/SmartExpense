package com.smartexpense.paycheck.effect

sealed interface PaycheckFormEffect {
    data object Saved : PaycheckFormEffect
    data object Deleted : PaycheckFormEffect
    data object RecurringStopped : PaycheckFormEffect
    data class ShowError(val message: String) : PaycheckFormEffect
}
