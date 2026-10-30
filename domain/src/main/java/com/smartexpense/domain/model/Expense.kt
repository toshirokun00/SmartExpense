package com.smartexpense.domain.model

import com.smartexpense.domain.money.Money
import java.math.BigDecimal

enum class ExpenseSyncStatus {
    PENDING,
    SYNCED,
    FAILED
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
            syncStatus: ExpenseSyncStatus = ExpenseSyncStatus.PENDING
        ) = Expense(
            id = id,
            amount = Money.toDouble(amountMinor),
            categoryId = categoryId,
            description = description,
            date = date,
            merchant = merchant,
            syncStatus = syncStatus
        )
    }
}
