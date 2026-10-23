package com.smartexpense.domain.usecase

import com.smartexpense.domain.budget.BudgetAlertEvaluation
import com.smartexpense.domain.budget.BudgetAlertEvaluator
import com.smartexpense.domain.model.Budget
import com.smartexpense.domain.model.BudgetAlertRecord
import com.smartexpense.domain.model.Expense

/**
 * Evaluates which budget alert thresholds have just been crossed for the given
 * budgets and expenses, and which delivery records must be added or removed.
 */
class EvaluateBudgetAlertsUseCase {

    operator fun invoke(
        budgets: List<Budget>,
        expenses: List<Expense>,
        thresholds: Set<Int>,
        previouslyDelivered: Set<BudgetAlertRecord>
    ): BudgetAlertEvaluation {
        return BudgetAlertEvaluator.evaluate(
            budgets = budgets,
            expenses = expenses,
            thresholds = thresholds,
            previouslyDelivered = previouslyDelivered
        )
    }
}
