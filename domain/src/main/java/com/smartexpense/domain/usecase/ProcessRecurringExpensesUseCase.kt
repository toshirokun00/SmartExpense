package com.smartexpense.domain.usecase

import com.smartexpense.domain.repository.RecurringExpenseRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Generates all due recurring occurrences for the signed-in user.
 *
 * The mutex serialises calls made by the foreground app, the periodic worker
 * and the immediate worker inside the same process. The repository and Room
 * transaction remain the source of truth, so duplicate work across processes
 * or retries is still safe.
 */
class ProcessRecurringExpensesUseCase(
    private val repository: RecurringExpenseRepository
) {

    private val mutex = Mutex()

    suspend operator fun invoke(): Int {
        return mutex.withLock {
            repository.processDueSchedules()
        }
    }
}
