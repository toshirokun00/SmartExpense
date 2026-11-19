package com.smartexpense.data

import com.smartexpense.data.auth.SupabaseAuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.status.RefreshFailureCause
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.plugins.PluginManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers the offline-first user resolution in [SupabaseAuthRepository].
 *
 * Without network supabase-kt cannot refresh an expired access token, reports
 * [SessionStatus.RefreshFailure] and `currentUserOrNull()` returns null even
 * though a session is still stored on device. Bills and paychecks are filtered
 * by the id returned here, so the repository must fall back to the stored
 * session instead of reporting an empty user.
 */
@OptIn(kotlin.time.ExperimentalTime::class)
class SupabaseAuthRepositoryTest {

    private val liveUser = UserInfo(
        aud = "authenticated",
        id = LIVE_USER_ID,
        email = "live@example.com"
    )

    private val storedUser = UserInfo(
        aud = "authenticated",
        id = STORED_USER_ID,
        email = "stored@example.com"
    )

    private fun session(user: UserInfo) = UserSession(
        accessToken = "access-token",
        refreshToken = "refresh-token",
        expiresIn = 3600,
        tokenType = "bearer",
        user = user
    )

    private class FakeSessionManager(private var stored: UserSession?) : SessionManager {
        override suspend fun saveSession(session: UserSession) {
            stored = session
        }

        override suspend fun loadSession(): UserSession? = stored

        override suspend fun deleteSession() {
            stored = null
        }
    }

    private fun repository(
        status: SessionStatus,
        storedSession: UserSession?,
        liveSession: UserSession?,
        auth: Auth = mockk(relaxed = true)
    ): SupabaseAuthRepository {
        every { auth.sessionStatus } returns MutableStateFlow(status)
        every { auth.sessionManager } returns FakeSessionManager(storedSession)
        every { auth.currentSessionOrNull() } returns liveSession
        every { auth.currentUserOrNull() } returns liveSession?.user

        val manager = PluginManager(mapOf(Auth.key to auth))
        val client = mockk<SupabaseClient> {
            every { pluginManager } returns manager
        }
        return SupabaseAuthRepository(client)
    }

    @Test
    fun `getCurrentUserId prefers the live session`() = runTest {
        val repository = repository(
            status = SessionStatus.Authenticated(session(liveUser)),
            storedSession = session(storedUser),
            liveSession = session(liveUser)
        )

        assertEquals(LIVE_USER_ID, repository.getCurrentUserId())
    }

    @Test
    fun `getCurrentUserId falls back to the stored session when refresh fails`() = runTest {
        val repository = repository(
            status = SessionStatus.RefreshFailure(REFRESH_FAILURE_CAUSE),
            storedSession = session(storedUser),
            liveSession = null
        )

        assertEquals(STORED_USER_ID, repository.getCurrentUserId())
    }

    @Test
    fun `getCurrentUserDetails works from the stored session while offline`() = runTest {
        val repository = repository(
            status = SessionStatus.RefreshFailure(REFRESH_FAILURE_CAUSE),
            storedSession = session(storedUser),
            liveSession = null
        )

        assertEquals("stored@example.com", repository.getCurrentUserDetails()?.email)
    }

    @Test
    fun `getCurrentUserId is null when nothing is stored`() = runTest {
        val repository = repository(
            status = SessionStatus.NotAuthenticated(),
            storedSession = null,
            liveSession = null
        )

        assertNull(repository.getCurrentUserId())
        assertNull(repository.getCurrentUserDetails())
    }

    @Test
    fun `signOut clears the local session when the network call fails`() = runTest {
        val auth = mockk<Auth>(relaxed = true)
        coEvery { auth.signOut() } throws IllegalStateException("no network")
        val repository = repository(
            status = SessionStatus.Authenticated(session(liveUser)),
            storedSession = session(liveUser),
            liveSession = session(liveUser),
            auth = auth
        )

        repository.signOut()

        coVerify(exactly = 1) { auth.clearSession() }
    }

    private companion object {
        const val LIVE_USER_ID = "11111111-1111-1111-1111-111111111111"
        const val STORED_USER_ID = "22222222-2222-2222-2222-222222222222"

        val REFRESH_FAILURE_CAUSE =
            RefreshFailureCause.NetworkError(IllegalStateException("offline"))
    }
}
