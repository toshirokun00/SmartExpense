package com.smartexpense.data

import androidx.work.ListenableWorker
import com.smartexpense.data.sync.RecurringExpenseWorker
import com.smartexpense.domain.usecase.ProcessRecurringExpensesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RecurringExpenseWorkerTest {

    private lateinit var processRecurringExpensesUseCase:
        ProcessRecurringExpensesUseCase
    private lateinit var worker: RecurringExpenseWorker

    @Before
    fun setup() {
        processRecurringExpensesUseCase = mockk()

        worker = RecurringExpenseWorker(
            appContext = mockk(relaxed = true),
            workerParams = mockk(relaxed = true),
            processRecurringExpensesUseCase =
                processRecurringExpensesUseCase
        )
    }

    @Test
    fun `successful processing returns success`() = runTest {
        coEvery {
            processRecurringExpensesUseCase()
        } returns 2

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) {
            processRecurringExpensesUseCase()
        }
    }

    @Test
    fun `transient failure returns retry`() = runTest {
        coEvery {
            processRecurringExpensesUseCase()
        } throws IllegalStateException("Database busy")

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `validation failure returns failure`() = runTest {
        coEvery {
            processRecurringExpensesUseCase()
        } throws IllegalArgumentException("Invalid schedule")

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
    }
}
