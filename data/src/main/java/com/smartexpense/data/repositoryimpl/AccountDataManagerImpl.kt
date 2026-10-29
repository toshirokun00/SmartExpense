package com.smartexpense.data.repositoryimpl

import android.content.Context
import com.smartexpense.database.AppDatabase
import com.smartexpense.domain.repository.AccountDataManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.content.edit

class AccountDataManagerImpl(
    private val context: Context,
    private val database: AppDatabase
) : AccountDataManager {

    override suspend fun onUserAuthenticated(userId: String) {
        val preferences = context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )
        val previousUserId = preferences.getString(KEY_LAST_USER_ID, null)

        if (previousUserId != null && previousUserId != userId) {
            // Local rows are not scoped by user, so the only safe way to show a
            // different account's data is to start from an empty database and
            // pull that account's rows from Supabase.
            withContext(Dispatchers.IO) {
                database.clearAllTables()
            }
        }

        preferences.edit {
            putString(KEY_LAST_USER_ID, userId)
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "account_session"
        const val KEY_LAST_USER_ID = "last_user_id"
    }
}
