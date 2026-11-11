package com.smartexpense.data.mapper

import com.smartexpense.database.entity.BillEntity
import com.smartexpense.database.entity.BillPaymentEntity
import com.smartexpense.domain.model.Bill
import com.smartexpense.domain.model.BillAmountType
import com.smartexpense.domain.model.BillFrequency
import com.smartexpense.domain.model.BillPayment
import com.smartexpense.domain.model.PaymentMethod

fun BillEntity.toDomain(): Bill = Bill(
    id = id,
    name = name,
    amount = amount,
    dueDateMillis = dueDate,
    notes = notes,
    frequency = BillFrequency.valueOf(frequency),
    repeatInterval = repeatInterval.coerceAtLeast(1),
    amountType = BillAmountType.valueOf(amountType),
    amountEstimated = amountEstimated,
    categoryId = categoryId,
    payee = payee,
    paymentMethod = safePaymentMethod(paymentMethod),
    autopay = autopay,
    reminderDaysBefore = reminderDaysBefore,
    reminderTimeMinutes = reminderTimeMinutes,
    paidAtMillis = paidAt,
    skipped = skipped,
    isActive = isActive
)

fun Bill.toEntity(userId: String): BillEntity = BillEntity(
    id = id,
    userId = userId,
    name = name,
    amount = amount,
    dueDate = dueDateMillis,
    isPaid = paidAtMillis != null,
    notes = notes,
    frequency = frequency.name,
    repeatInterval = repeatInterval.coerceAtLeast(1),
    amountType = amountType.name,
    amountEstimated = amountEstimated,
    categoryId = categoryId,
    payee = payee,
    paymentMethod = paymentMethod.name,
    autopay = autopay,
    reminderDaysBefore = reminderDaysBefore,
    reminderTimeMinutes = reminderTimeMinutes,
    paidAt = paidAtMillis,
    skipped = skipped,
    isActive = isActive
)

fun BillPaymentEntity.toDomain(): BillPayment = BillPayment(
    id = id,
    billId = billId,
    billName = billName,
    amount = amount,
    paidAtMillis = paidAt,
    paymentMethod = safePaymentMethod(paymentMethod)
)

private fun safePaymentMethod(value: String): PaymentMethod =
    runCatching { PaymentMethod.valueOf(value) }.getOrDefault(PaymentMethod.CASH)
