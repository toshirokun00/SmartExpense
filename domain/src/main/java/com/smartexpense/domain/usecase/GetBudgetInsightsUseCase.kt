package com.smartexpense.domain.usecase

import com.smartexpense.domain.budget.BudgetInsightGenerator
import com.smartexpense.domain.model.Budget
import com.smartexpense.domain.model.BudgetInsight
import com.smartexpense.domain.model.Category
import com.smartexpense.domain.model.Expense

/**
 * Produces deterministic spending insights from locally available budget and
 * expense data. No network or external service is involved.
 */
class GetBudgetInsightsUseCase {

    operator fun invoke(
        budgets: List<Budget>,
        expenses: List<Expense>,
        categories: List<Category>,
        warningThreshold: Int,
        now: Long = System.currentTimeMillis()
    ): List<BudgetInsight> {
        return BudgetInsightGenerator.generate(
            budgets = budgets,
            expenses = expenses,
            categories = categories,
            warningThreshold = warningThreshold,
            now = now
        )
    }
}
