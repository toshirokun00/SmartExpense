package com.smartexpense.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabasePaycheckDto(
    @SerialName("client_id")
    val clientId: String,
    @SerialName("user_id")
    val userId: String,
    val amount: Double,
    @SerialName("pay_date")
    val payDate: String,
    val label: String = "",
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("schedule_client_id")
    val scheduleClientId: String? = null,
    @SerialName("occurrence_date")
    val occurrenceDate: String? = null,
    @SerialName("is_skipped")
    val isSkipped: Boolean = false
)

@Serializable
data class SupabasePaycheckResponseDto(
    val id: Long,
    @SerialName("client_id")
    val clientId: String,
    @SerialName("user_id")
    val userId: String,
    val amount: Double,
    @SerialName("pay_date")
    val payDate: String,
    val label: String = "",
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("schedule_client_id")
    val scheduleClientId: String? = null,
    @SerialName("occurrence_date")
    val occurrenceDate: String? = null,
    @SerialName("is_skipped")
    val isSkipped: Boolean = false
)

@Serializable
data class SupabasePaycheckBillDto(
    @SerialName("client_id")
    val clientId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("paycheck_client_id")
    val paycheckClientId: String,
    @SerialName("bill_client_id")
    val billClientId: String,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class SupabasePaycheckBillResponseDto(
    val id: Long,
    @SerialName("client_id")
    val clientId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("paycheck_client_id")
    val paycheckClientId: String,
    @SerialName("bill_client_id")
    val billClientId: String,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class SupabasePaycheckScheduleDto(
    @SerialName("client_id")
    val clientId: String,
    @SerialName("user_id")
    val userId: String,
    val amount: Double,
    val label: String = "",
    val frequency: String,
    @SerialName("first_day")
    val firstDay: Int,
    @SerialName("second_day")
    val secondDay: Int? = null,
    @SerialName("start_date")
    val startDate: String,
    @SerialName("end_date")
    val endDate: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class SupabasePaycheckScheduleResponseDto(
    val id: Long,
    @SerialName("client_id")
    val clientId: String,
    @SerialName("user_id")
    val userId: String,
    val amount: Double,
    val label: String = "",
    val frequency: String,
    @SerialName("first_day")
    val firstDay: Int,
    @SerialName("second_day")
    val secondDay: Int? = null,
    @SerialName("start_date")
    val startDate: String,
    @SerialName("end_date")
    val endDate: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("created_at")
    val createdAt: String
)
