package com.smartexpense.domain.budget

import com.smartexpense.domain.model.Budget
import com.smartexpense.domain.model.BudgetAlert
import com.smartexpense.domain.model.BudgetAlertRecord
import com.smartexpense.domain.model.Expense

/**
 * Result of an alert evaluation pass.
 *
 * @param alerts        alerts that should be delivered now (i.e. a threshold was
 *                      just crossed).
 * @param delivered     records to persist so the same alert is not delivered twice.
 * @param removeDelivered records to delete because spending dropped back below a
 *                      threshold (enabling a future re-cross) or because a budget
 *                      was removed / its period changed.
 */
data class BudgetAlertEvaluation(
    val alerts: List<BudgetAlert>,
    val delivered: List<BudgetAlertRecord>,
    val removeDelivered: List<BudgetAlertRecord>
)

/**
 * Pure threshold-crossing logic for budget alerts.
 *
 * Alerts fire only when utilization crosses a configured threshold. If spending
 * later falls back below a threshold the corresponding record is removed so a
 * genuine future re-cross can alert again.
 */
object BudgetAlertEvaluator {

    fun evaluate(
        budgets: List<Budget>,
        expenses: List<Expense>,
        thresholds: Set<Int>,
        previouslyDelivered: Set<BudgetAlertRecord>
    ): BudgetAlertEvaluation {

        val activeThresholds = thresholds.filter { it > 0 }.sorted()
        val alerts = mutableListOf<BudgetAlert>()
        val delivered = mutableListOf<BudgetAlertRecord>()
        val remove = linkedSetOf<BudgetAlertRecord>()

        val periodByBudget = budgets.associate { it.id to (it.startDate to it.endDate) }

        // Drop history for budgets that were deleted or whose period changed.
        previouslyDelivered.forEach { record ->
            val period = periodByBudget[record.budgetId]
            if (period == null ||
                period.first != record.periodStart ||
                period.second != record.periodEnd
            ) {
                remove += record
            }
        }

        budgets.forEach { budget ->
            val spent = BudgetCalculator.spentAmount(budget, expenses)
            val utilization = BudgetCalculator.utilizationPercentage(spent, budget.amount)

            activeThresholds.forEach { threshold ->
                val record = BudgetAlertRecord(
                    budgetId = budget.id,
                    periodStart = budget.startDate,
                    periodEnd = budget.endDate,
                    threshold = threshold
                )
                val alreadyDelivered = record in previouslyDelivered

                if (utilization >= threshold) {
                    if (!alreadyDelivered) {
                        alerts += BudgetAlert(
                            budgetId = budget.id,
                            categoryId = budget.categoryId,
                            threshold = threshold,
                            spentAmount = spent,
                            budgetAmount = budget.amount,
                            utilizationPercentage = utilization
                        )
                        delivered += record
                    }
                } else if (alreadyDelivered) {
                    remove += record
                }
            }
        }

        return BudgetAlertEvaluation(
            alerts = alerts,
            delivered = delivered,
            removeDelivered = remove.toList()
        )
    }
}
