package com.smartexpense.data

import androidx.work.ListenableWorker
import com.smartexpense.data.sync.ExpenseSyncWorker
import com.smartexpense.domain.repository.ExpenseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ExpenseSyncWorkerTest {

    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var worker: ExpenseSyncWorker

    @Before
    fun setup() {
        expenseRepository = mockk()

        worker = ExpenseSyncWorker(
            appContext = mockk(relaxed = true),
            workerParams = mockk(relaxed = true),
            expenseRepository = expenseRepository
        )
    }

    @Test
    fun `successful sync returns success`() = runTest {
        coEvery {
            expenseRepository.syncPendingExpenses()
        } returns Unit

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)

        coVerify(exactly = 1) {
            expenseRepository.syncPendingExpenses()
        }
    }

    @Test
    fun `sync failure returns retry`() = runTest {
        coEvery {
            expenseRepository.syncPendingExpenses()
        } throws IllegalStateException("Network error")

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.retry(), result)

        coVerify(exactly = 1) {
            expenseRepository.syncPendingExpenses()
        }
    }
}