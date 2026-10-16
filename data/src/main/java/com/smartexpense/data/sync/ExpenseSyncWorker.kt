package com.smartexpense.data.sync

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartexpense.domain.repository.ExpenseRepository
import kotlinx.coroutines.CancellationException

class ExpenseSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val expenseRepository: ExpenseRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Expense sync worker started")

        return try {
            expenseRepository.syncPendingExpenses()
            Log.d(TAG, "Expense sync completed successfully")
            Result.success()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.e(TAG, "Expense sync failed", exception)
            Result.retry()
        }
    }
    companion object {
        private const val TAG = "ExpenseSyncWorker"
    }
}