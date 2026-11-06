package com.smartexpense.domain.repository

import com.smartexpense.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {

    fun observeProfile(userId: String): Flow<UserProfile?>

    suspend fun saveProfile(userId: String, username: String, avatarUri: String?)

    suspend fun deleteProfile(userId: String)
}
