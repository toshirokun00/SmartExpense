package com.smartexpense.domain.usecase

import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.repository.ExpenseRepository

class AddExpenseUseCase(
    private val expenseRepository: ExpenseRepository
) {
    suspend operator fun invoke(expense : Expense) {
        expenseRepository.addExpense(expense)
    }
}