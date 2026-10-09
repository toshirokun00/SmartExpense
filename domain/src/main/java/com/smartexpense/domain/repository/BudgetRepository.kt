package com.smartexpense.domain.repository

import com.smartexpense.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {

    fun getBudgets(): Flow<List<Budget>>

    suspend fun getBudgetById(
        id: Long
    ): Budget?

    suspend fun addBudget(
        budget: Budget
    )

    suspend fun updateBudget(
        budget: Budget
    )

    suspend fun deleteBudget(
        budget: Budget
    )
}