package com.smartexpense.domain.usecase

import com.smartexpense.domain.model.UserProfile
import com.smartexpense.domain.repository.AuthRepository
import com.smartexpense.domain.repository.ProfileRepository
import com.smartexpense.domain.repository.ProfileRemoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.random.Random

class ProfileUseCase(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val profileRemoteRepository: ProfileRemoteRepository
) {

    /**
     * Observes local profile changes; on first collect, merges the remote
     * profile (if available) so the local room table stays in sync after
     * a fresh install / account switch.
     */
    suspend fun observeProfileForCurrentUser(): Flow<UserProfile?> {
        val userId = authRepository.getCurrentUserId()

        // Merge: subscribe to remote changes and upsert into local Room so
        // the local flow stays authoritative on subsequent emissions.
        val remoteFlow = profileRemoteRepository.usernameChanged
            .map { remote ->
                userId?.let { uid ->
                    profileRepository.saveProfile(
                        userId = uid,
                        username = remote.username.orEmpty(),
                        avatarUri = remote.avatarUri
                    )
                }
                remote
            }

        val localFlow = if (userId != null) {
            profileRepository.observeProfile(userId)
        } else {
            kotlinx.coroutines.flow.flowOf(null)
        }

        return combine(localFlow, remoteFlow) { local, remote ->
            val localProfile = local?.let {
                UserProfile(
                    userId = it.userId,
                    username = it.username,
                    avatarUri = it.avatarUri
                )
            }
            when {
                // Prefer the locally-observed profile when present.
                local != null -> localProfile
                // Fall back to remote when local is empty (e.g. after a fresh
                // install where hydrateLocalFromRemote hasn't run yet).
                remote != null -> UserProfile(
                    userId = userId.orEmpty(),
                    username = remote.username.orEmpty(),
                    avatarUri = remote.avatarUri
                )
                else -> null
            }
        }
    }

    /**
     * Reads the local Room profile for the current user. No remote
     * involvement — safe for UI observers that just need the username
     * for the dashboard greeting. Returns `null` if no user is signed in
     * or no profile exists yet.
     */
    suspend fun observeLocalProfileForCurrentUser(): com.smartexpense.domain.model.UserProfile? {
        val userId = authRepository.getCurrentUserId() ?: return null
        return profileRepository.observeProfile(userId).first()
    }

    /**
     * Saves the profile locally for instant UI feedback, then pushes it to the
     * remote backend. When a `content://` avatar is uploaded, the returned
     * public URL replaces the device-only URI in the local store so the avatar
     * survives a restart or reinstall.
     *
     * @return the avatar URI that should be shown to the user (the public URL
     * after a successful upload).
     */
    suspend fun saveProfile(username: String, avatarUri: String?): String? {
        val userId = authRepository.getCurrentUserId()
            ?: return avatarUri
        // 1) Persist locally so the UI reflects the change immediately.
        profileRepository.saveProfile(
            userId = userId,
            username = username,
            avatarUri = avatarUri
        )
        // 2) Push to remote. Upload errors propagate to the caller so the UI
        //    can tell the user the avatar was not uploaded.
        val resolvedAvatar = profileRemoteRepository.saveProfile(
            username = username,
            avatarUri = avatarUri
        )
        val durableAvatar = resolvedAvatar?.takeIf {
            it.startsWith("http://") || it.startsWith("https://")
        }
        if (durableAvatar != null && durableAvatar != avatarUri) {
            profileRepository.saveProfile(
                userId = userId,
                username = username,
                avatarUri = durableAvatar
            )
        }
        return durableAvatar ?: avatarUri
    }

    /**
     * Removes the current user's avatar while keeping the username. The local
     * avatar is cleared first so the UI immediately falls back to the initial
     * placeholder, then the remote storage object and metadata are cleared.
     */
    suspend fun removeAvatar(username: String) {
        val userId = authRepository.getCurrentUserId()
            ?: return
        profileRepository.saveProfile(
            userId = userId,
            username = username,
            avatarUri = null
        )
        profileRemoteRepository.saveProfile(
            username = username.takeIf { it.isNotBlank() },
            avatarUri = null,
            deleteAvatar = true
        )
    }

    suspend fun deleteProfile() {
        val userId = authRepository.getCurrentUserId()
            ?: return
        profileRepository.deleteProfile(userId)
        profileRemoteRepository.saveProfile(
            username = null,
            avatarUri = null,
            deleteAvatar = true
        )
    }

    suspend fun getCurrentUserDetails() = authRepository.getCurrentUserDetails()

    /**
     * Returns the user's public Spendora ID (for example `spendora4821`),
     * creating and storing it in Supabase metadata the first time it is
     * needed. Returns null when the user is signed out or the remote update
     * fails, in which case the caller can fall back to its cached value.
     */
    suspend fun ensureSpendoraId(): String? {
        authRepository.getCurrentUserId() ?: return null

        val existing = profileRemoteRepository.loadSpendoraId()
            ?.takeIf { it.startsWith(SPENDORA_ID_PREFIX, ignoreCase = true) }
        if (existing != null) return existing

        val generated = "$SPENDORA_ID_PREFIX${Random.nextInt(1, 1_000_000)}"
        return try {
            profileRemoteRepository.saveSpendoraId(generated)
            generated
        } catch (exception: Exception) {
            null
        }
    }

    /** Reads the email visibility preference saved on the remote account. */
    suspend fun loadEmailVisibility(): Boolean? {
        authRepository.getCurrentUserId() ?: return null
        return profileRemoteRepository.loadEmailVisibility()
    }

    /** Saves the email visibility preference so other devices can restore it. */
    suspend fun saveEmailVisibility(visible: Boolean) {
        authRepository.getCurrentUserId() ?: return
        profileRemoteRepository.saveEmailVisibility(visible)
    }

    /**
     * Pulls remote profile data. Call this when the app starts on a fresh
     * install or when the user signs in so we hydrate the local DB with
     * the user's previously saved profile.
     *
     * If no remote profile exists yet, clears the local profile for the
     * current user so stale data from a previous session doesn't linger.
     */
    suspend fun hydrateLocalFromRemote() {
        val userId = authRepository.getCurrentUserId() ?: return
        val remote = profileRemoteRepository.loadProfile()
        if (remote != null) {
            profileRepository.saveProfile(
                userId = userId,
                username = remote.username.orEmpty(),
                avatarUri = remote.avatarUri
            )
        } else {
            // No remote profile — clear local so a stale username from a
            // previous session doesn't show.
            profileRepository.deleteProfile(userId)
        }
    }

    private companion object {
        const val SPENDORA_ID_PREFIX = "spendora"
    }
}
