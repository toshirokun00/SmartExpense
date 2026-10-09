package com.smartexpense.domain.usecase

import com.smartexpense.domain.model.AppSettings
import com.smartexpense.domain.model.AppTheme
import com.smartexpense.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class SettingsUseCase(
    private val repository: SettingsRepository
) {
    fun getSettings(): Flow<AppSettings> {
        return repository.getSettings()
    }

    suspend fun setTheme(theme: AppTheme) {
        repository.setTheme(theme)
    }

    suspend fun setNotificationsEnabled(
        enabled: Boolean
    ) {
        repository.setNotificationsEnabled(enabled)
    }
}