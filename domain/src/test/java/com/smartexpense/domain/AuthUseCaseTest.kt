package com.smartexpense.domain

import com.smartexpense.domain.repository.AuthRepository
import com.smartexpense.domain.usecase.AuthUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class AuthUseCaseTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var authUseCase: AuthUseCase

    @Before
    fun setUp() {
        authRepository = mockk()
        authUseCase = AuthUseCase(authRepository)
    }

    @Test
    fun signInWithValidCredentialsCallsRepository() = runTest {
        coEvery {
            authRepository.signIn("user@example.com", "password123")
        } returns Unit

        authUseCase.signIn(" user@example.com ", "password123")

        coVerify(exactly = 1) {
            authRepository.signIn("user@example.com", "password123")
        }
    }

    @Test
    fun signInWithBlankEmailThrowsException() = runTest {
        try {
            authUseCase.signIn(" ", "password123")
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Email is required", exception.message)
        }

        coVerify(exactly = 0) {
            authRepository.signIn(any(), any())
        }
    }

    @Test
    fun signInWithBlankPasswordThrowsException() = runTest {
        try {
            authUseCase.signIn("user@example.com", " ")
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals("Password is required", exception.message)
        }

        coVerify(exactly = 0) {
            authRepository.signIn(any(), any())
        }
    }

    @Test
    fun signUpWithValidCredentialsCallsRepository() = runTest {
        coEvery {
            authRepository.signUp("user@example.com", "password123")
        } returns Unit

        authUseCase.signUp(" user@example.com ", "password123")

        coVerify(exactly = 1) {
            authRepository.signUp("user@example.com", "password123")
        }
    }

    @Test
    fun signUpWithShortPasswordThrowsException() = runTest {
        try {
            authUseCase.signUp("user@example.com", "short")
            fail("Expected IllegalArgumentException")
        } catch (exception: IllegalArgumentException) {
            assertEquals(
                "Password must be at least 8 characters",
                exception.message
            )
        }

        coVerify(exactly = 0) {
            authRepository.signUp(any(), any())
        }
    }

    @Test
    fun signOutCallsRepository() = runTest {
        coEvery { authRepository.signOut() } returns Unit

        authUseCase.signOut()

        coVerify(exactly = 1) {
            authRepository.signOut()
        }
    }

    @Test
    fun getCurrentUserIdReturnsRepositoryValue() = runTest {
        coEvery {
            authRepository.getCurrentUserId()
        } returns "user-123"

        assertEquals("user-123", authUseCase.getCurrentUserId())
    }

    @Test
    fun getCurrentUserIdReturnsNullWhenSignedOut() = runTest {
        coEvery {
            authRepository.getCurrentUserId()
        } returns null

        assertNull(authUseCase.getCurrentUserId())
    }
}