package com.smartexpense.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartexpense.domain.usecase.ProcessRecurringExpensesUseCase
import kotlin.coroutines.cancellation.CancellationException

/**
 * Background entry point for recurring expense generation.
 *
 * Processing is local and does not require network connectivity. Any generated
 * expenses are picked up by the existing expense sync worker, which enqueues
 * itself separately with a network constraint.
 */
class RecurringExpenseWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val processRecurringExpensesUseCase: ProcessRecurringExpensesUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            processRecurringExpensesUseCase()
            Result.success()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: IllegalArgumentException) {
            // Invalid schedule data cannot be fixed by retrying.
            Log.e(TAG, "Recurring expense processing failed validation", exception)
            Result.failure()
        } catch (exception: Exception) {
            Log.e(TAG, "Recurring expense processing failed", exception)
            Result.retry()
        }
    }

    private companion object {
        const val TAG = "RecurringExpenseWorker"
    }
}
