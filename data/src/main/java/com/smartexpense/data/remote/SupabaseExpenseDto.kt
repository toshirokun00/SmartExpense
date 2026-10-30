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

    @SerialName("expense_date")
    val expenseDate: String
)
