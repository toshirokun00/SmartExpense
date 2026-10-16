package com.smartexpense.data.sync

import android.content.Context

class WorkManagerExpenseSyncEnqueuer(
    private val scheduler: ExpenseSyncScheduler
) : ExpenseSyncEnqueuer {

    override fun enqueue() {
        scheduler.enqueue()
    }
}