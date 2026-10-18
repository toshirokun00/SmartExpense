package com.smartexpense.domain.usecase

import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.model.SearchCriteria
import com.smartexpense.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow

class SearchExpensesUseCase(
    private val expenseRepository: ExpenseRepository
) {
    operator fun invoke(criteria: SearchCriteria): Flow<List<Expense>> {
        return expenseRepository.searchExpenses(criteria)
    }
}
