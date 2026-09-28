package com.smartexpense.domain.usecase

import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.repository.ExpenseRepository

class GetExpenseByIdUseCase(
    private val expenseRepository: ExpenseRepository
) {
    suspend operator fun invoke(id : Long) : Expense? {
        return expenseRepository.getExpenseById(id)
    }
}