package com.smartexpense.domain.usecase

import com.smartexpense.domain.repository.SyncEnqueuer

/**
 * Requests a background sync. Used after authentication so a user's existing
 * Supabase data is restored into the local database.
 */
class ScheduleSyncUseCase(
    private val syncEnqueuer: SyncEnqueuer
) {

    operator fun invoke() {
        syncEnqueuer.enqueueAfterCurrent()
    }
}
