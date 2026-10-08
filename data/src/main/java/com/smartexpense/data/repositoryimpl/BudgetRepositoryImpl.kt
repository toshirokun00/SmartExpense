package com.smartexpense.data.repositoryimpl

import com.smartexpense.data.mapper.toDomain
import com.smartexpense.data.mapper.toEntity
import com.smartexpense.database.dao.BudgetDao
import com.smartexpense.domain.model.Budget
import com.smartexpense.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BudgetRepositoryImpl(
    private val budgetDao: BudgetDao
) : BudgetRepository {

    override fun getBudgets(): Flow<List<Budget>> {
        return budgetDao.getBudgets()
            .map { budgets ->
                budgets.map {
                    it.toDomain()
                }
            }
    }

    override suspend fun getBudgetById(
        id: Long
    ): Budget? {
        return budgetDao
            .getBudgetById(id)
            ?.toDomain()
    }

    override suspend fun addBudget(
        budget: Budget
    ) {
        budgetDao.insertBudget(
            budget.toEntity()
        )
    }

    override suspend fun updateBudget(
        budget: Budget
    ) {
        budgetDao.updateBudget(
            budget.toEntity()
        )
    }

    override suspend fun deleteBudget(
        budget: Budget
    ) {
        budgetDao.deleteBudget(
            budget.toEntity()
        )
    }
}