package com.smartexpense.domain.model

import com.smartexpense.domain.money.Money
import java.math.BigDecimal

enum class ExpenseSyncStatus {
    PENDING,
    SYNCED,
    FAILED;

    fun label(): String = when (this) {
        PENDING -> "Pending"
        SYNCED -> "Synced"
        FAILED -> "Failed"
    }
}

enum class ExpenseType(val displayName: String) {
    EXPENSE("Expense"),
    INCOME("Income"),
    REFUND("Refund")
}

enum class PaymentMethod(val displayName: String) {
    CASH("Cash"),
    CREDIT_CARD("Credit Card"),
    DEBIT_CARD("Debit Card"),
    BANK_TRANSFER("Bank Transfer"),
    E_WALLET("E-Wallet"),
    OTHER("Other")
}

/**
 * [amount] remains a [Double] for compatibility with the existing budget,
 * report, and dashboard calculations. [amountMinor] is the authoritative value
 * for persistence: it is derived from [amount] and stored as integer centavos.
 * New expenses should be built with [fromMinorUnits] so no precision is lost at
 * the input boundary.
 */
data class Expense(
    val id: Long = 0L,
    val amount: Double,
    val categoryId: Long,
    val description: String,
    val date: Long,
    val merchant: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val expenseType: ExpenseType = ExpenseType.EXPENSE,
    val tags: List<String> = emptyList(),
    val notes: String = "",
    val syncStatus: ExpenseSyncStatus = ExpenseSyncStatus.PENDING
) {
    val amountMinor: Long
        get() = Money.toMinorUnits(amount)

    val exactAmount: BigDecimal
        get() = Money.fromMinorUnits(amountMinor)

    companion object {
        fun fromMinorUnits(
            id: Long = 0L,
            amountMinor: Long,
            categoryId: Long,
            description: String,
            date: Long,
            merchant: String = "",
            paymentMethod: PaymentMethod = PaymentMethod.CASH,
            expenseType: ExpenseType = ExpenseType.EXPENSE,
            tags: List<String> = emptyList(),
            notes: String = "",
            syncStatus: ExpenseSyncStatus = ExpenseSyncStatus.PENDING
        ) = Expense(
            id = id,
            amount = Money.toDouble(amountMinor),
            categoryId = categoryId,
            description = description,
            date = date,
            merchant = merchant,
            paymentMethod = paymentMethod,
            expenseType = expenseType,
            tags = tags,
            notes = notes,
            syncStatus = syncStatus
        )
    }
}
