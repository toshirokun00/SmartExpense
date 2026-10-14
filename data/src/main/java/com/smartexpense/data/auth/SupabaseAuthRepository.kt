package com.smartexpense.data.auth


import com.smartexpense.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email

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
        supabaseClient.auth.signUpWith(Email) {
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
}