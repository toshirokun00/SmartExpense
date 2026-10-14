package com.smartexpense.domain.repository

interface AuthRepository {

    suspend fun signIn(
        email: String,
        password: String
    )

    suspend fun signUp(
        email: String,
        password: String
    )

    suspend fun signOut()

    suspend fun getCurrentUserId(): String?
}