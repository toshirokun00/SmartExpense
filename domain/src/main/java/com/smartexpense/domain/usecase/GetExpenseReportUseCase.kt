package com.smartexpense.domain.usecase

import com.smartexpense.domain.model.CategorySpending
import com.smartexpense.domain.model.DailySpending
import com.smartexpense.domain.model.ExpenseReport
import com.smartexpense.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId

class GetExpenseReportUseCase(
    private val expenseRepository: ExpenseRepository
) {
    operator fun invoke(
        startDateInclusive: Long,
        endDateExclusive: Long
    ): Flow<ExpenseReport> {
        require(endDateExclusive > startDateInclusive) {
            "End date must be after start date"
        }

        return expenseRepository.getExpense().map { expenses ->
            val filteredExpenses = expenses.filter { expense ->
                expense.date in startDateInclusive..<endDateExclusive
            }

            val totalAmount = filteredExpenses.sumOf { it.amount }

            val categoryBreakdown = filteredExpenses
                .groupBy { it.categoryId }
                .map { (categoryId, categoryExpenses) ->
                    val categoryTotal = categoryExpenses.sumOf { it.amount }

                    CategorySpending(
                        categoryId = categoryId,
                        totalAmount = categoryTotal,
                        percentage = if (totalAmount > 0.0) {
                            categoryTotal / totalAmount * 100.0
                        } else {
                            0.0
                        }
                    )
                }
                .sortedByDescending { it.totalAmount }

            val dailySpending = filteredExpenses
                .groupBy { expense ->
                    Instant.ofEpochMilli(expense.date)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                }
                .map { (date, dayExpenses) ->
                    DailySpending(
                        date = date,
                        totalAmount = dayExpenses.sumOf { it.amount }
                    )
                }
                .sortedBy { it.date }

            ExpenseReport(
                totalAmount = totalAmount,
                expenseCount = filteredExpenses.size,
                averageAmount = if (filteredExpenses.isNotEmpty()) {
                    totalAmount / filteredExpenses.size
                } else {
                    0.0
                },
                categoryBreakdown = categoryBreakdown,
                dailySpending = dailySpending
            )
        }
    }
}