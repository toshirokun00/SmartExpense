package com.smartexpense.domain.repository

/**
 * Schedules a background sync of the user's data (push pending local changes and
 * pull the latest remote data).
 */
interface SyncEnqueuer {
    fun enqueue()
}
