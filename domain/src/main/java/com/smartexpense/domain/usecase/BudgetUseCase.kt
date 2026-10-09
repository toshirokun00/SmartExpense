package com.smartexpense.domain.usecase

import com.smartexpense.domain.model.Budget
import com.smartexpense.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow

class BudgetUseCase(
    private val repository: BudgetRepository
) {

    fun getBudgets(): Flow<List<Budget>> {
        return repository.getBudgets()
    }

    suspend fun getBudgetById(
        id: Long
    ): Budget? {
        return repository.getBudgetById(id)
    }

    suspend fun addBudget(
        budget: Budget
    ) {
        repository.addBudget(budget)
    }

    suspend fun updateBudget(
        budget: Budget
    ) {
        repository.updateBudget(budget)
    }

    suspend fun deleteBudget(
        budget: Budget
    ) {
        repository.deleteBudget(budget)
    }
}