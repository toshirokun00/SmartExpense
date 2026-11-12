package com.smartexpense.data.repositoryimpl

import com.smartexpense.data.remote.SupabaseProfileRemoteDataSource
import com.smartexpense.domain.repository.ProfileRemoteRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onCompletion

class SupabaseProfileRemoteRepository(
    private val remoteDataSource: SupabaseProfileRemoteDataSource
) : ProfileRemoteRepository {

    private val _usernameChanged = MutableSharedFlow<ProfileRemoteRepository.RemoteProfile>(
        replay = 0,
        extraBufferCapacity = 4
    )
    override val usernameChanged: SharedFlow<ProfileRemoteRepository.RemoteProfile> =
        _usernameChanged

    override suspend fun saveProfile(
        username: String?,
        avatarUri: String?,
        deleteAvatar: Boolean
    ): String? {
        val resolvedAvatar = remoteDataSource.saveProfile(
            username = username,
            avatarUri = avatarUri,
            deleteAvatar = deleteAvatar
        )
        // Publish to observers. Re-fetch to reflect the post-save state.
        val latest = remoteDataSource.loadProfile()
        if (latest != null) {
            _usernameChanged.emit(
                ProfileRemoteRepository.RemoteProfile(
                    username = latest.username,
                    avatarUri = resolvedAvatar ?: latest.avatarUri,
                    spendoraId = latest.spendoraId,
                    isEmailVisible = latest.isEmailVisible
                )
            )
        }
        return resolvedAvatar ?: latest?.avatarUri
    }

    override suspend fun loadProfile(): ProfileRemoteRepository.RemoteProfile? {
        val data = remoteDataSource.loadProfile()
        return data?.let {
            ProfileRemoteRepository.RemoteProfile(
                username = it.username,
                avatarUri = it.avatarUri,
                spendoraId = it.spendoraId,
                isEmailVisible = it.isEmailVisible
            )
        }
    }

    override suspend fun loadSpendoraId(): String? =
        remoteDataSource.loadSpendoraId()

    override suspend fun saveSpendoraId(spendoraId: String) {
        remoteDataSource.saveSpendoraId(spendoraId)
    }

    override suspend fun loadEmailVisibility(): Boolean? =
        remoteDataSource.loadEmailVisibility()

    override suspend fun saveEmailVisibility(visible: Boolean) {
        remoteDataSource.saveEmailVisibility(visible)
    }
}
