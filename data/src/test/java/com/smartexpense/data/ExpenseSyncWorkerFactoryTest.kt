package com.smartexpense.data

import android.content.Context
import androidx.work.WorkerParameters
import com.smartexpense.data.sync.ExpenseSyncWorker
import com.smartexpense.data.sync.ExpenseSyncWorkerFactory
import com.smartexpense.data.sync.RecurringExpenseWorker
import com.smartexpense.domain.repository.ExpenseRepository
import com.smartexpense.domain.usecase.ProcessRecurringExpensesUseCase
import io.mockk.mockk
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExpenseSyncWorkerFactoryTest {

    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var processRecurringExpensesUseCase:
        ProcessRecurringExpensesUseCase
    private lateinit var factory: ExpenseSyncWorkerFactory

    @Before
    fun setup() {
        expenseRepository = mockk()
        processRecurringExpensesUseCase = mockk()

        factory = ExpenseSyncWorkerFactory(
            expenseRepository = expenseRepository,
            processRecurringExpensesUseCase =
                processRecurringExpensesUseCase
        )
    }

    @Test
    fun `creates ExpenseSyncWorker`() {
        val worker = factory.createWorker(
            appContext = mockk<Context>(relaxed = true),
            workerClassName = ExpenseSyncWorker::class.java.name,
            workerParameters = mockk<WorkerParameters>(relaxed = true)
        )

        assertTrue(worker is ExpenseSyncWorker)
    }

    @Test
    fun `creates RecurringExpenseWorker`() {
        val worker = factory.createWorker(
            appContext = mockk<Context>(relaxed = true),
            workerClassName = RecurringExpenseWorker::class.java.name,
            workerParameters = mockk<WorkerParameters>(relaxed = true)
        )

        assertTrue(worker is RecurringExpenseWorker)
    }

    @Test
    fun `returns null for unknown workers`() {
        val worker = factory.createWorker(
            appContext = mockk<Context>(relaxed = true),
            workerClassName = "com.smartexpense.UnknownWorker",
            workerParameters = mockk<WorkerParameters>(relaxed = true)
        )

        assertNull(worker)
    }
}
