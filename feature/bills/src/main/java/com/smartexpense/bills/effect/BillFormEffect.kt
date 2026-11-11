package com.smartexpense.bills.effect

/** One-off results emitted by the bill form so the screen can navigate back. */
sealed interface BillFormEffect {
    data object Saved : BillFormEffect

    data object Deleted : BillFormEffect

    data object Skipped : BillFormEffect

    data class ShowError(val message: String) : BillFormEffect
}
