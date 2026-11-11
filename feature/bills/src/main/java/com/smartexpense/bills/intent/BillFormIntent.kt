package com.smartexpense.bills.intent

import com.smartexpense.domain.model.BillAmountType
import com.smartexpense.domain.model.BillFrequency
import com.smartexpense.domain.model.PaymentMethod

sealed interface BillFormIntent {
    data class NameChanged(val value: String) : BillFormIntent
    data class AmountChanged(val value: String) : BillFormIntent
    data class DueDateChanged(val millis: Long) : BillFormIntent
    data class NotesChanged(val value: String) : BillFormIntent
    data class RecurringChanged(val value: Boolean) : BillFormIntent
    data class FrequencyChanged(val value: BillFrequency) : BillFormIntent
    data class IntervalChanged(val value: String) : BillFormIntent
    data class AmountTypeChanged(val value: BillAmountType) : BillFormIntent
    data class CategoryChanged(val id: Long?) : BillFormIntent
    data class PayeeChanged(val value: String) : BillFormIntent
    data class PaymentMethodChanged(val value: PaymentMethod) : BillFormIntent
    data class AutopayChanged(val value: Boolean) : BillFormIntent
    data class ReminderDaysToggled(val days: Int) : BillFormIntent
    data class ReminderTimeChanged(val minutes: Int) : BillFormIntent
    data class ActiveChanged(val value: Boolean) : BillFormIntent

    data object Save : BillFormIntent
    data object Delete : BillFormIntent
    data object Skip : BillFormIntent
}
