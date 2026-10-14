package com.smartexpense.domain.usecase

import com.smartexpense.domain.repository.AuthRepository

class AuthUseCase(
    private val authRepository: AuthRepository
) {

    suspend fun signIn(
        email: String,
        password: String
    ) {
        require(email.isNotBlank()) {
            "Email is required"
        }
        require(password.isNotBlank()) {
            "Password is required"
        }

        authRepository.signIn(
            email = email.trim(),
            password = password
        )
    }

    suspend fun signUp(
        email: String,
        password: String
    ) {
        require(email.isNotBlank()) {
            "Email is required"
        }
        require(password.length >= 8) {
            "Password must be at least 8 characters"
        }

        authRepository.signUp(
            email = email.trim(),
            password = password
        )
    }

    suspend fun signOut() {
        authRepository.signOut()
    }

    suspend fun getCurrentUserId(): String? {
        return authRepository.getCurrentUserId()
    }
}