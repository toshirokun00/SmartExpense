package com.smartexpense.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseBillDto(
    @SerialName("client_id")
    val clientId: String,
    @SerialName("user_id")
    val userId: String,
    val name: String,
    val amount: Double,
    @SerialName("due_date")
    val dueDate: String,
    @SerialName("category_id")
    val categoryId: Long? = null,
    val notes: String = "",
    val frequency: String = "NONE",
    @SerialName("repeat_interval")
    val repeatInterval: Int = 1,
    @SerialName("amount_type")
    val amountType: String = "STATIC",
    @SerialName("amount_estimated")
    val amountEstimated: Boolean = false,
    val payee: String = "",
    @SerialName("payment_method")
    val paymentMethod: String = "CASH",
    val autopay: Boolean = false,
    @SerialName("reminder_days_before")
    val reminderDaysBefore: Int = 3,
    @SerialName("reminder_time_minutes")
    val reminderTimeMinutes: Int = 540,
    @SerialName("paid_at")
    val paidAt: String? = null,
    val skipped: Boolean = false,
    @SerialName("is_active")
    val isActive: Boolean = true
)

@Serializable
data class SupabaseBillResponseDto(
    val id: Long,
    @SerialName("client_id")
    val clientId: String,
    @SerialName("user_id")
    val userId: String,
    val name: String,
    val amount: Double,
    @SerialName("due_date")
    val dueDate: String,
    @SerialName("category_id")
    val categoryId: Long? = null,
    val notes: String = "",
    val frequency: String = "NONE",
    @SerialName("repeat_interval")
    val repeatInterval: Int = 1,
    @SerialName("amount_type")
    val amountType: String = "STATIC",
    @SerialName("amount_estimated")
    val amountEstimated: Boolean = false,
    val payee: String = "",
    @SerialName("payment_method")
    val paymentMethod: String = "CASH",
    val autopay: Boolean = false,
    @SerialName("reminder_days_before")
    val reminderDaysBefore: Int = 3,
    @SerialName("reminder_time_minutes")
    val reminderTimeMinutes: Int = 540,
    @SerialName("paid_at")
    val paidAt: String? = null,
    val skipped: Boolean = false,
    @SerialName("is_active")
    val isActive: Boolean = true
)

@Serializable
data class SupabaseBillPaymentDto(
    @SerialName("client_id")
    val clientId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("bill_client_id")
    val billClientId: String,
    @SerialName("bill_name")
    val billName: String = "",
    val amount: Double,
    @SerialName("paid_at")
    val paidAt: String,
    @SerialName("payment_method")
    val paymentMethod: String = "CASH",
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class SupabaseBillPaymentResponseDto(
    val id: Long,
    @SerialName("client_id")
    val clientId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("bill_client_id")
    val billClientId: String,
    @SerialName("bill_name")
    val billName: String = "",
    val amount: Double,
    @SerialName("paid_at")
    val paidAt: String,
    @SerialName("payment_method")
    val paymentMethod: String = "CASH",
    @SerialName("created_at")
    val createdAt: String
)
