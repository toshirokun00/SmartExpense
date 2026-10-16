package com.smartexpense.database

import androidx.room.withTransaction
import com.smartexpense.database.entity.PendingExpenseDeletionEntity

class ExpenseDeletionTransaction(
    private val database: AppDatabase
) {
    suspend fun deleteExpense(
        expenseId: Long,
        clientId: String
    ) {
        database.withTransaction {
            database.pendingExpenseDeletionDao().insert(
                PendingExpenseDeletionEntity(clientId = clientId)
            )

            database.expenseDao().deleteExpenseById(expenseId)
        }
    }
}