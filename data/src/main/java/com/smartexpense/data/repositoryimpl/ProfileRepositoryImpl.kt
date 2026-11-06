package com.smartexpense.data.repositoryimpl

import com.smartexpense.database.dao.UserProfileDao
import com.smartexpense.database.entity.UserProfileEntity
import com.smartexpense.domain.model.UserProfile
import com.smartexpense.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepositoryImpl(
    private val userProfileDao: UserProfileDao
) : ProfileRepository {

    override fun observeProfile(userId: String): Flow<UserProfile?> {
        return userProfileDao.getProfileForUser(userId)
            .map { entity ->
                entity?.let {
                    UserProfile(
                        userId = it.userId,
                        username = it.username,
                        avatarUri = it.avatarUri
                    )
                }
            }
    }

    override suspend fun saveProfile(userId: String, username: String, avatarUri: String?) {
        userProfileDao.upsertProfile(
            UserProfileEntity(
                userId = userId,
                username = username,
                avatarUri = avatarUri
            )
        )
    }

    override suspend fun deleteProfile(userId: String) {
        userProfileDao.deleteProfileForUser(userId)
    }
}
