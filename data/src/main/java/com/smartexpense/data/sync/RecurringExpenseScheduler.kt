package com.smartexpense.data.sync

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules recurring expense processing on the existing WorkManager instance.
 *
 * The periodic request is the background safety net. Immediate requests are
 * enqueued after schedules change and when the app starts/resumes, so due
 * occurrences are processed promptly without exact alarms.
 */
class RecurringExpenseScheduler(
    private val context: Context
) {

    fun enqueueImmediate() {
        val request = OneTimeWorkRequestBuilder<RecurringExpenseWorker>()
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }

    fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<RecurringExpenseWorker>(
            PERIODIC_INTERVAL_HOURS,
            TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    companion object {
        const val IMMEDIATE_WORK_NAME = "recurring_expenses_immediate"
        const val PERIODIC_WORK_NAME = "recurring_expenses_periodic"
        const val PERIODIC_INTERVAL_HOURS = 6L
    }
}
