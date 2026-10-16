package com.smartexpense.data.sync

interface ExpenseSyncEnqueuer {
    fun enqueue()
}

class ExpenseSyncEnqueuerImpl(
    private val scheduler: ExpenseSyncScheduler
) : ExpenseSyncEnqueuer {

    override fun enqueue() {
        scheduler.enqueue()
    }
}