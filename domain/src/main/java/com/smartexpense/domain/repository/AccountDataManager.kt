package com.smartexpense.domain.repository

/**
 * Prepares the local database for the account that just signed in.
 *
 * The app keeps one account's data on the device at a time. When a different
 * account is used, the previous account's rows are removed so its expenses,
 * categories, and budgets can never leak into the new account.
 */
interface AccountDataManager {

    suspend fun onUserAuthenticated(userId: String)
}
