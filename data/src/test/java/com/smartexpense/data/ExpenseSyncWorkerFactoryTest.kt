package com.smartexpense.data

import android.content.Context
import androidx.work.WorkerParameters
import com.smartexpense.data.sync.ExpenseSyncWorker
import com.smartexpense.data.sync.ExpenseSyncWorkerFactory
import com.smartexpense.domain.repository.ExpenseRepository
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExpenseSyncWorkerFactoryTest {

    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var factory: ExpenseSyncWorkerFactory

    @Before
    fun setup() {
        expenseRepository = mockk()
        factory = ExpenseSyncWorkerFactory(
            expenseRepository = expenseRepository
        )
    }

    @Test
    fun `creates ExpenseSyncWorker`() {
        val context = mockk<Context>(relaxed = true)
        val workerParameters = mockk<WorkerParameters>(relaxed = true)

        val worker = factory.createWorker(
            appContext = context,
            workerClassName = ExpenseSyncWorker::class.java.name,
            workerParameters = workerParameters
        )

        assertTrue(worker is ExpenseSyncWorker)
    }
}