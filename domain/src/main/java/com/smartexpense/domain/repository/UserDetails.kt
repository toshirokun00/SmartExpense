package com.smartexpense.domain.repository

/**
 * Lightweight snapshot of the currently authenticated user's account info.
 */
data class UserDetails(
    val email: String,
    val memberSince: String
)
