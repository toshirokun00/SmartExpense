package com.smartexpense.database

import androidx.room.withTransaction
import com.smartexpense.database.entity.PendingBudgetDeletionEntity

class BudgetDeletionTransaction(
    private val database: AppDatabase
) {
    suspend fun deleteBudget(
        budgetId: Long,
        clientId: String
    ) {
        database.withTransaction {
            database.pendingBudgetDeletionDao().insert(
                PendingBudgetDeletionEntity(clientId = clientId)
            )

            database.budgetDao().deleteBudgetById(budgetId)
        }
    }
}
