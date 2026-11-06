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

    suspend fun saveProfile(username: String, avatarUri: String?) {
        val userId = authRepository.getCurrentUserId()
            ?: return
        // 1) Persist locally so the UI reflects the change immediately.
        profileRepository.saveProfile(
            userId = userId,
            username = username,
            avatarUri = avatarUri
        )
        // 2) Push to remote. Fire-and-forget on the caller's scope is fine;
        //    the remote layer tolerates offline failures.
        profileRemoteRepository.saveProfile(
            username = username,
            avatarUri = avatarUri
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
}
