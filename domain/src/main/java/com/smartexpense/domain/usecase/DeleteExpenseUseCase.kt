package com.smartexpense.domain.usecase

import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.repository.ExpenseRepository

class DeleteExpenseUseCase(
    private val expenseRepository: ExpenseRepository
) {
    suspend operator fun invoke(expense : Expense) {
        expenseRepository.deleteExpense(expense)
    }
}