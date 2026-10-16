package com.smartexpense.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseCategoryDto(
    @SerialName("client_id")
    val clientId: String,
    @SerialName("user_id")
    val userId: String,
    val name: String
)

@Serializable
data class SupabaseCategoryResponseDto(
    val id: Long,
    @SerialName("client_id")
    val clientId: String,
    val name: String
)