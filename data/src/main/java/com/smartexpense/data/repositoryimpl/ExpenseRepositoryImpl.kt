package com.smartexpense.data.repositoryimpl

import com.smartexpense.data.repositoryimpl.mapper.toDomain
import com.smartexpense.data.repositoryimpl.mapper.toEntity
import com.smartexpense.database.dao.ExpenseDao
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExpenseRepositoryImpl(
    private val expenseDao: ExpenseDao
)  : ExpenseRepository {
    override fun getExpense(): Flow<List<Expense>> {
        return expenseDao.getExpenses().map { expenses ->
            expenses.map {
                it.toDomain()
            }
        }
    }

    override suspend fun getExpenseById(id: Long): Expense? {
        return expenseDao.getExpenseById(id)?.toDomain()
    }

    override suspend fun addExpense(expense: Expense) {
       expenseDao.insertExpense(expense.toEntity())
    }

    override suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense.toEntity())
    }

    override suspend fun deleteExpense(expense: Expense) {
       expenseDao.deleteExpense(expense.toEntity())
    }
}