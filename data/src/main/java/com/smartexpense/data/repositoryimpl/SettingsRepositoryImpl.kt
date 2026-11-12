package com.smartexpense.data.repositoryimpl

import com.smartexpense.core.common.SettingsDataStore
import com.smartexpense.domain.model.AppSettings
import com.smartexpense.domain.model.AppTheme
import com.smartexpense.domain.model.BudgetAlertThresholds
import com.smartexpense.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class SettingsRepositoryImpl(
    private val dataStore: SettingsDataStore
) : SettingsRepository {
    override fun getSettings(): Flow<AppSettings> {
        return combine(
            dataStore.theme,
            dataStore.notificationsEnabled,
            dataStore.budgetAlertThresholds,
            dataStore.emailVisible,
            dataStore.spendoraId
        ) { theme, notificationsEnabled, thresholds, emailVisible, spendoraId ->

            AppSettings(
                theme = when (theme) {
                    "LIGHT" -> AppTheme.LIGHT
                    "DARK" -> AppTheme.DARK
                    else -> AppTheme.SYSTEM
                },
                notificationsEnabled = notificationsEnabled,
                budgetAlertThresholds = thresholds
                    ?.let { BudgetAlertThresholds.normalize(it) }
                    ?: BudgetAlertThresholds.DEFAULT,
                isEmailVisible = emailVisible,
                spendoraId = spendoraId
            )
        }
    }

    override suspend fun setTheme(theme: AppTheme) {
        dataStore.setTheme(theme.name)
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.setNotificationsEnabled(enabled)
    }

    override suspend fun setBudgetAlertThresholds(thresholds: Set<Int>) {
        dataStore.setBudgetAlertThresholds(
            BudgetAlertThresholds.normalize(thresholds)
        )
    }

    override suspend fun setEmailVisible(visible: Boolean) {
        dataStore.setEmailVisible(visible)
    }

    override suspend fun setSpendoraId(spendoraId: String) {
        dataStore.setSpendoraId(spendoraId)
    }
}
