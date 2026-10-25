package com.smartexpense.domain.usecase

import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Returns local expenses using the same inclusive-start/exclusive-end
 * boundary used by the Reports feature.
 */
class GetExpensesInRangeUseCase(
    private val expenseRepository: ExpenseRepository
) {
    operator fun invoke(
        startDateInclusive: Long,
        endDateExclusive: Long
    ): Flow<List<Expense>> {
        require(endDateExclusive > startDateInclusive) {
            "End date must be after start date"
        }

        return expenseRepository.getExpense().map { expenses ->
            expenses
                .filter { expense ->
                    expense.date in startDateInclusive..<endDateExclusive
                }
                .sortedWith(compareBy<Expense> { it.date }.thenBy { it.id })
        }
    }
}
