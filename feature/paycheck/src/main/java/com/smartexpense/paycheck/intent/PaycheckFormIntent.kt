package com.smartexpense.paycheck.intent

import com.smartexpense.domain.model.PaycheckFrequency

sealed interface PaycheckFormIntent {
    data class AmountChanged(val value: String) : PaycheckFormIntent
    data class PayDateChanged(val millis: Long) : PaycheckFormIntent
    data class LabelChanged(val value: String) : PaycheckFormIntent
    data class BillToggled(val billId: Long) : PaycheckFormIntent

    data class RecurringChanged(val enabled: Boolean) : PaycheckFormIntent
    data class FrequencyChanged(val frequency: PaycheckFrequency) : PaycheckFormIntent
    data class FirstDayChanged(val day: Int) : PaycheckFormIntent
    data class SecondDayChanged(val day: Int) : PaycheckFormIntent

    data object StopRepeating : PaycheckFormIntent
    data object Save : PaycheckFormIntent
    data object Delete : PaycheckFormIntent
}
