package com.smartexpense.core.common

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
    }

        val theme: Flow<String> =
            context.settingsDataStore.data.map { preferences ->
                preferences[THEME] ?: "SYSTEM"
            }

        val notificationsEnabled: Flow<Boolean> =
            context.settingsDataStore.data.map { preferences ->
                preferences[NOTIFICATIONS_ENABLED] ?: true
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
}