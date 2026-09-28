package com.smartexpense.domain.usecase

import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow

class GetExpenseUseCase(
    private val expenseRepository: ExpenseRepository
) {
    operator fun invoke() : Flow<List<Expense>> {
        return expenseRepository.getExpense()

    }
}