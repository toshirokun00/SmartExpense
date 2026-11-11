package com.smartexpense.data.sync

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.smartexpense.database.dao.BillDao
import com.smartexpense.domain.repository.AuthRepository
import com.smartexpense.domain.repository.ExpenseRepository
import com.smartexpense.domain.usecase.ProcessRecurringExpensesUseCase

class ExpenseSyncWorkerFactory(
    private val expenseRepository: ExpenseRepository,
    private val processRecurringExpensesUseCase: ProcessRecurringExpensesUseCase,
    private val billDao: BillDao,
    private val authRepository: AuthRepository
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

            RecurringExpenseWorker::class.java.name ->
                RecurringExpenseWorker(
                    appContext = appContext,
                    workerParams = workerParameters,
                    processRecurringExpensesUseCase = processRecurringExpensesUseCase
                )

            BillReminderWorker::class.java.name ->
                BillReminderWorker(
                    appContext = appContext,
                    params = workerParameters,
                    billDao = billDao,
                    authRepository = authRepository
                )

            else -> null
        }
    }
}
