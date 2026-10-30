package com.smartexpense.feature.expenses.intent

import com.smartexpense.domain.model.ExpenseType
import com.smartexpense.domain.model.PaymentMethod

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

    data class PaymentMethodChanged(val paymentMethod: PaymentMethod) : ExpenseIntent

    data class ExpenseTypeChanged(val expenseType: ExpenseType) : ExpenseIntent

    data class TagsChanged(val tags: String) : ExpenseIntent

    data class NotesChanged(val notes: String) : ExpenseIntent

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
