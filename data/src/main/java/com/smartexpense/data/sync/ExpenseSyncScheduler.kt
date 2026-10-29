package com.smartexpense.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class ExpenseSyncScheduler(
    private val context: Context
) {

    fun enqueue() {
        enqueue(ExistingWorkPolicy.KEEP)
    }

    fun enqueueAfterCurrent() {
        enqueue(ExistingWorkPolicy.APPEND_OR_REPLACE)
    }

    private fun enqueue(policy: ExistingWorkPolicy) {
        val workManager = WorkManager.getInstance(context)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<ExpenseSyncWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            policy,
            request
        )
    }

    companion object {
        const val UNIQUE_WORK_NAME = "expense_sync"
    }
}
