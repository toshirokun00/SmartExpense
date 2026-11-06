package com.smartexpense.domain.model

data class UserProfile(
    val userId: String,
    val username: String = "",
    val avatarUri: String? = null,
    val email: String = "",
    val memberSince: String = ""
)
