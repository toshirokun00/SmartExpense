package com.smartexpense.data.repositoryimpl

import com.smartexpense.core.common.SettingsDataStore
import com.smartexpense.domain.model.AppSettings
import com.smartexpense.domain.model.AppTheme
import com.smartexpense.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class SettingsRepositoryImpl(
    private val dataStore: SettingsDataStore
) : SettingsRepository {
    override fun getSettings(): Flow<AppSettings> {
        return combine(
            dataStore.theme,
            dataStore.notificationsEnabled
        ) { theme, notificationsEnabled ->

            AppSettings(
                theme = when (theme) {
                    "LIGHT" -> AppTheme.LIGHT
                    "DARK" -> AppTheme.DARK
                    else -> AppTheme.SYSTEM
                },
                notificationsEnabled = notificationsEnabled
            )
        }
    }

    override suspend fun setTheme(theme: AppTheme) {
        dataStore.setTheme(theme.name)
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.setNotificationsEnabled(enabled)
    }
}