package com.smartexpense.data.auth


import com.smartexpense.domain.repository.AuthRepository
import com.smartexpense.domain.repository.UserDetails
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import java.time.format.DateTimeFormatter

class SupabaseAuthRepository(
    private val supabaseClient: SupabaseClient
) : AuthRepository {

    override suspend fun signIn(
        email: String,
        password: String
    ) {
        supabaseClient.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signUp(
        email: String,
        password: String
    ) {
        supabaseClient.auth.signUpWith(Email,
            redirectUrl = "com.smartexpense://auth/callback") {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signOut() {
        supabaseClient.auth.signOut()
    }

    override suspend fun getCurrentUserId(): String? {
        supabaseClient.auth.awaitInitialization()
        return supabaseClient.auth.currentUserOrNull()?.id
    }

    @OptIn(kotlin.time.ExperimentalTime::class)
    override suspend fun getCurrentUserDetails(): UserDetails? {
        supabaseClient.auth.awaitInitialization()
        val user = supabaseClient.auth.currentUserOrNull() ?: return null
        val memberSince = user.createdAt?.let { createdAt ->
            DateTimeFormatter
                .ofPattern("MMM d, yyyy")
                .withZone(java.time.ZoneId.systemDefault())
                .format(java.time.Instant.ofEpochSecond(createdAt.epochSeconds))
        }.orEmpty()
        return UserDetails(
            email = user.email.orEmpty(),
            memberSince = memberSince
        )
    }
}