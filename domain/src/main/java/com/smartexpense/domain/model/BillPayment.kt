package com.smartexpense.domain.model

/**
 * A recorded bill payment. Kept separately from [Bill] so paying a recurring
 * bill can roll the bill forward to its next due date without losing the
 * history needed for trends and averages.
 */
data class BillPayment(
    val id: Long = 0L,
    val billId: Long,
    val billName: String = "",
    val amount: Double,
    val paidAtMillis: Long,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH
)
