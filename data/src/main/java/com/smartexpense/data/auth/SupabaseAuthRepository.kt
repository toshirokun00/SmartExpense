package com.smartexpense.data.auth


import com.smartexpense.domain.repository.AuthRepository
import com.smartexpense.domain.repository.UserDetails
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import java.time.format.DateTimeFormatter
import kotlin.coroutines.cancellation.CancellationException

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
        try {
            supabaseClient.auth.signOut()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            // Offline first: supabase-kt calls the logout endpoint before it
            // clears the local session, so a network failure would otherwise
            // leave the cached session signed in. Drop it locally either way.
            supabaseClient.auth.clearSession()
        }
    }

    override suspend fun getCurrentUserId(): String? =
        currentUserOrStored()?.id

    @OptIn(kotlin.time.ExperimentalTime::class)
    override suspend fun getCurrentUserDetails(): UserDetails? {
        val user = currentUserOrStored() ?: return null
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

    /**
     * The signed-in user used for local (Room) reads.
     *
     * Lives in the live session when there is one, but falls back to the
     * session stored on device. Without network supabase-kt cannot refresh an
     * expired access token, reports `SessionStatus.RefreshFailure`, and
     * `currentUserOrNull()` turns null even though the user is still signed in
     * locally. Bills and paychecks are filtered by this id, so the fallback
     * keeps those screens working offline.
     *
     * The stored session is deleted on sign-out and when a refresh token is
     * rejected, so another account's rows are never returned.
     */
    private suspend fun currentUserOrStored(): UserInfo? {
        supabaseClient.auth.currentUserOrNull()?.let { return it }
        return runCatching {
            supabaseClient.auth.sessionManager.loadSession()?.user
        }.getOrNull()
    }
}
