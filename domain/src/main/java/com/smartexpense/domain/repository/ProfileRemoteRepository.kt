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
     *
     * @return the final avatar URI (the uploaded public URL when a new image
     * was uploaded), or null when there is nothing to store.
     */
    suspend fun saveProfile(
        username: String?,
        avatarUri: String?,
        deleteAvatar: Boolean = false
    ): String?

    /**
     * Fetches the current remote profile. May return `null` when no data
     * exists yet or when the request fails (offline, not authenticated).
     */
    suspend fun loadProfile(): RemoteProfile?

    /**
     * Reads the user's public Spendora account ID (for example `spendora4821`).
     * Returns null when it has not been generated yet.
     */
    suspend fun loadSpendoraId(): String?

    /**
     * Persists the user's public Spendora account ID in the remote profile
     * metadata so every device displays the same value.
     */
    suspend fun saveSpendoraId(spendoraId: String)

    /** Reads the user's saved email visibility preference. */
    suspend fun loadEmailVisibility(): Boolean?

    /** Saves the user's email visibility preference for other devices. */
    suspend fun saveEmailVisibility(visible: Boolean)

    /**
     * Hot stream of remote profile changes. Consumers that observe this will
     * be notified whenever a profile update succeeds on the remote side.
     */
    val usernameChanged: SharedFlow<RemoteProfile>

    data class RemoteProfile(
        val username: String?,
        val avatarUri: String?,
        val spendoraId: String? = null,
        val isEmailVisible: Boolean? = null
    )
}
