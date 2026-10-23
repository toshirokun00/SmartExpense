package com.smartexpense.core.common

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.smartexpense.core.common.SettingsDataStore.Keys.BUDGET_ALERT_THRESHOLDS
import com.smartexpense.core.common.SettingsDataStore.Keys.BUDGET_ALERT_THRESHOLDS_CONFIGURED
import com.smartexpense.core.common.SettingsDataStore.Keys.NOTIFICATIONS_ENABLED
import com.smartexpense.core.common.SettingsDataStore.Keys.THEME
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


private val Context.settingsDataStore by preferencesDataStore(
    name = "settings"
)

class SettingsDataStore(
    private val context: Context
)  {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val BUDGET_ALERT_THRESHOLDS = stringSetPreferencesKey("budget_alert_thresholds")
        val BUDGET_ALERT_THRESHOLDS_CONFIGURED =
            booleanPreferencesKey("budget_alert_thresholds_configured")
    }

        val theme: Flow<String> =
            context.settingsDataStore.data.map { preferences ->
                preferences[THEME] ?: "SYSTEM"
            }

        val notificationsEnabled: Flow<Boolean> =
            context.settingsDataStore.data.map { preferences ->
                preferences[NOTIFICATIONS_ENABLED] ?: true
            }

        /**
         * Persisted budget alert thresholds.
         *
         * Emits `null` when the user has never configured thresholds so the
         * repository can apply its defaults. Once configured, an empty set is a
         * valid value meaning "no budget alerts" and is emitted as-is.
         */
        val budgetAlertThresholds: Flow<Set<Int>?> =
            context.settingsDataStore.data.map { preferences ->
                val configured =
                    preferences[BUDGET_ALERT_THRESHOLDS_CONFIGURED] == true ||
                        preferences.contains(BUDGET_ALERT_THRESHOLDS)

                if (!configured) {
                    null
                } else {
                    preferences[BUDGET_ALERT_THRESHOLDS]
                        ?.mapNotNull { it.toIntOrNull() }
                        ?.toSet()
                        ?: emptySet()
                }
            }

        suspend fun setTheme(theme: String) {
            context.settingsDataStore.edit { preferences ->
                preferences[THEME] = theme
            }
        }

        suspend fun setNotificationsEnabled(enabled: Boolean) {
            context.settingsDataStore.edit { preferences ->
                preferences[NOTIFICATIONS_ENABLED] = enabled
            }
        }

        suspend fun setBudgetAlertThresholds(thresholds: Set<Int>) {
            context.settingsDataStore.edit { preferences ->
                preferences[BUDGET_ALERT_THRESHOLDS] =
                    thresholds.map { it.toString() }.toSet()
                preferences[BUDGET_ALERT_THRESHOLDS_CONFIGURED] = true
            }
        }
}
