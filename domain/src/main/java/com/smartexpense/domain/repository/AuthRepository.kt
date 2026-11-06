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

    /**
     * Returns the signed-in user's email and account creation date (formatted).
     * Both may be null when no user is signed in.
     */
    suspend fun getCurrentUserDetails(): UserDetails?
}