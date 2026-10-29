package com.smartexpense.domain.repository

/**
 * Schedules a background sync of the user's data (push pending local changes and
 * pull the latest remote data).
 */
interface SyncEnqueuer {

    fun enqueue()

    /**
     * Schedules a sync behind any work already queued under the same name.
     *
     * Used after sign-in so a newly authenticated account's restore is not
     * dropped because another sync request is still queued or running.
     */
    fun enqueueAfterCurrent() {
        enqueue()
    }
}
