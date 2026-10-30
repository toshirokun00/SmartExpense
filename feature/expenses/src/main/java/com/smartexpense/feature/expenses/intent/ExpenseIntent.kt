package com.smartexpense.feature.expenses.intent

sealed interface ExpenseIntent {


    data class LoadExpense(
        val id: Long
    ) : ExpenseIntent

    data object AddExpenses : ExpenseIntent

    data class UpdateExpense(
        val id: Long
    ) : ExpenseIntent

    data class AmountChanged(
        val amount: String
    ) : ExpenseIntent

    data class DescriptionChanged(val description: String) : ExpenseIntent

    data class MerchantChanged(val merchant: String) : ExpenseIntent

    data class DateChanged(val date: Long) : ExpenseIntent

    data class CategoryChanged(val categoryId: Long) : ExpenseIntent

    data class RequestDeleteExpense(
        val id: Long
    ) : ExpenseIntent

    data class RetrySync(
        val id: Long
    ) : ExpenseIntent

    data object ConfirmDeleteExpense : ExpenseIntent

    data object CancelDeleteExpense : ExpenseIntent

}
