package com.smartexpense.domain.repository

import com.smartexpense.domain.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {

    fun getExpense() : Flow<List<Expense>>

    suspend fun getExpenseById(id : Long): Expense?

    suspend fun addExpense(expense: Expense)

    suspend fun updateExpense(expense: Expense)

    suspend fun deleteExpense(expense: Expense)
}