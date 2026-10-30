package com.smartexpense.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseExpenseDto(
    @SerialName("client_id")
    val clientId: String,

    @SerialName("user_id")
    val userId: String,

    val amount: Double,

    @SerialName("category_id")
    val categoryId: Long,

    val description: String,

    val merchant: String = "",

    @SerialName("payment_method")
    val paymentMethod: String = "CASH",

    @SerialName("expense_type")
    val expenseType: String = "EXPENSE",

    val tags: String = "",

    val notes: String = "",

    @SerialName("expense_date")
    val expenseDate: String
)

@Serializable
data class SupabaseExpenseResponseDto(
    val id: Long,

    @SerialName("client_id")
    val clientId: String,

    @SerialName("user_id")
    val userId: String,

    val amount: Double,

    @SerialName("category_id")
    val categoryId: Long,

    val description: String,

    // Nullable so rows created before the merchant column existed still decode.
    val merchant: String? = null,

    @SerialName("payment_method")
    val paymentMethod: String? = null,

    @SerialName("expense_type")
    val expenseType: String? = null,

    val tags: String? = null,

    val notes: String? = null,

    @SerialName("expense_date")
    val expenseDate: String
)
