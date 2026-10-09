package com.smartexpense.domain.repository

import com.smartexpense.domain.model.AppSettings
import com.smartexpense.domain.model.AppTheme
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    fun getSettings(): Flow<AppSettings>

    suspend fun setTheme(theme: AppTheme)

    suspend fun setNotificationsEnabled(enabled: Boolean)
}