package com.smartexpense.data.sync

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.smartexpense.domain.repository.ExpenseRepository
class ExpenseSyncWorkerFactory(
    private val expenseRepository: ExpenseRepository
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            ExpenseSyncWorker::class.java.name ->
                ExpenseSyncWorker(
                    appContext = appContext,
                    workerParams = workerParameters,
                    expenseRepository = expenseRepository
                )

            else -> null
        }
    }
}