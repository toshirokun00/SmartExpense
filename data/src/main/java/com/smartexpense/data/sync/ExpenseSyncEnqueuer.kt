package com.smartexpense.data.sync

import com.smartexpense.domain.repository.SyncEnqueuer

interface ExpenseSyncEnqueuer : SyncEnqueuer

class ExpenseSyncEnqueuerImpl(
    private val scheduler: ExpenseSyncScheduler
) : ExpenseSyncEnqueuer {

    override fun enqueue() {
        scheduler.enqueue()
    }
}