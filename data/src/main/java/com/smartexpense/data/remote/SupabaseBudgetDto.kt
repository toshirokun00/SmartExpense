package com.smartexpense.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseBudgetDto(
    @SerialName("client_id")
    val clientId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("category_id")
    val categoryId: Long,
    val amount: Double,
    @SerialName("start_date")
    val startDate: String,
    @SerialName("end_date")
    val endDate: String
)

@Serializable
data class SupabaseBudgetResponseDto(
    val id: Long,
    @SerialName("client_id")
    val clientId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("category_id")
    val categoryId: Long,
    val amount: Double,
    @SerialName("start_date")
    val startDate: String,
    @SerialName("end_date")
    val endDate: String
)
