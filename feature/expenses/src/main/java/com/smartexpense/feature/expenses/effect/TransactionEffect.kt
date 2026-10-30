package com.smartexpense.feature.expenses.effect

sealed interface TransactionEffect {

    data class ShowError(val message: String) : TransactionEffect

    data object SyncRetryRequested : TransactionEffect
}
