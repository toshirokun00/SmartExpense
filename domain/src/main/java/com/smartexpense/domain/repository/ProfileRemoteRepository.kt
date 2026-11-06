package com.smartexpense.domain.repository

import kotlinx.coroutines.flow.SharedFlow

/**
 * Handles profile data persistence in a remote backend (Supabase in this
 * implementation). Exposes both one-shot read/write and an event stream so
 * consumers can react to remote changes without polling.
 */
interface ProfileRemoteRepository {

    /**
     * Saves the profile to the remote backend. [avatarUri], when a `content://`
     * URI, is first uploaded to remote storage. A null [avatarUri] is a no-op;
     * set [deleteAvatar] to explicitly clear it.
     */
    suspend fun saveProfile(
        username: String?,
        avatarUri: String?,
        deleteAvatar: Boolean = false
    )

    /**
     * Fetches the current remote profile. May return `null` when no data
     * exists yet or when the request fails (offline, not authenticated).
     */
    suspend fun loadProfile(): RemoteProfile?

    /**
     * Hot stream of remote profile changes. Consumers that observe this will
     * be notified whenever a profile update succeeds on the remote side.
     */
    val usernameChanged: SharedFlow<RemoteProfile>

    data class RemoteProfile(
        val username: String?,
        val avatarUri: String?
    )
}
