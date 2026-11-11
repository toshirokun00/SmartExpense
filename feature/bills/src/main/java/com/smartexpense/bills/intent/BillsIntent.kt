package com.smartexpense.bills.intent

import com.smartexpense.domain.model.Bill
import com.smartexpense.domain.model.PaymentMethod

sealed interface BillsIntent {
    data object Load : BillsIntent
    data class ShowPaySheet(val bill: Bill) : BillsIntent
    data object HidePaySheet : BillsIntent
    data class Pay(
        val id: Long,
        val amount: Double,
        val paidAtMillis: Long,
        val paymentMethod: PaymentMethod,
        val createExpense: Boolean
    ) : BillsIntent
}
