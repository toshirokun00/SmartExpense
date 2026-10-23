package com.smartexpense.budget.alerts

import com.smartexpense.core.common.toPesoAmount
import com.smartexpense.domain.model.AppSettings
import com.smartexpense.domain.model.Budget
import com.smartexpense.domain.model.BudgetAlert
import com.smartexpense.domain.model.BudgetAlertRecord
import com.smartexpense.domain.model.BudgetAlertThresholds
import com.smartexpense.domain.model.Category
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.repository.BudgetAlertRepository
import com.smartexpense.domain.usecase.BudgetUseCase
import com.smartexpense.domain.usecase.CategoryUseCase
import com.smartexpense.domain.usecase.EvaluateBudgetAlertsUseCase
import com.smartexpense.domain.usecase.GetExpenseUseCase
import com.smartexpense.domain.usecase.SettingsUseCase
import com.smartexpense.notification.ExpenseNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Observes budgets, expenses, categories and settings and delivers budget alerts
 * when a configured utilization threshold is crossed.
 *
 * The design deliberately avoids WorkManager: Room is the source of truth and
 * exposes reactive Flows, so every expense/budget change (including changes pulled
 * in by background synchronization) triggers a re-evaluation. Deduplication via
 * [BudgetAlertRepository] keeps repeated evaluations idempotent.
 */
class BudgetAlertCoordinator(
    private val getExpenseUseCase: GetExpenseUseCase,
    private val budgetUseCase: BudgetUseCase,
    private val categoryUseCase: CategoryUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val evaluateBudgetAlertsUseCase: EvaluateBudgetAlertsUseCase,
    private val budgetAlertRepository: BudgetAlertRepository,
    private val notificationManager: ExpenseNotificationManager
) {

    private data class Snapshot(
        val budgets: List<Budget>,
        val expenses: List<Expense>,
        val categories: List<Category>,
        val settings: AppSettings,
        val delivered: Set<BudgetAlertRecord>
    )

    fun start(scope: CoroutineScope): Job {
        return scope.launch {
            combine(
                budgetUseCase.getBudgets(),
                getExpenseUseCase(),
                categoryUseCase.getCategories(),
                settingsUseCase.getSettings(),
                budgetAlertRepository.getDeliveredAlerts()
            ) { budgets, expenses, categories, settings, delivered ->
                Snapshot(
                    budgets = budgets,
                    expenses = expenses,
                    categories = categories,
                    settings = settings,
                    delivered = delivered.toSet()
                )
            }.collect { snapshot ->
                evaluate(snapshot)
            }
        }
    }

    /**
     * Evaluates the current snapshot and delivers notifications for newly crossed
     * thresholds. Kept private; [start] drives it from the observed flows.
     */
    private suspend fun evaluate(snapshot: Snapshot) {
        if (!snapshot.settings.notificationsEnabled) {
            return
        }

        val thresholds = BudgetAlertThresholds.normalize(
            snapshot.settings.budgetAlertThresholds
        )

        if (thresholds.isEmpty()) {
            return
        }

        val evaluation = evaluateBudgetAlertsUseCase(
            budgets = snapshot.budgets,
            expenses = snapshot.expenses,
            thresholds = thresholds,
            previouslyDelivered = snapshot.delivered
        )

        evaluation.alerts.forEach { alert ->
            deliver(alert, snapshot.categories)
        }

        if (evaluation.delivered.isNotEmpty()) {
            budgetAlertRepository.recordDeliveredAlerts(evaluation.delivered)
        }

        if (evaluation.removeDelivered.isNotEmpty()) {
            budgetAlertRepository.removeDeliveredAlerts(evaluation.removeDelivered)
        }
    }

    private fun deliver(alert: BudgetAlert, categories: List<Category>) {
        val categoryName = categories
            .firstOrNull { it.id == alert.categoryId }
            ?.name
            ?: "Unknown"

        val title = if (alert.threshold >= 100) {
            "Budget exceeded: $categoryName"
        } else {
            "Budget alert: $categoryName"
        }

        val utilization = alert.utilizationPercentage.toInt()

        val message = if (alert.threshold >= 100) {
            "You've spent ${alert.spentAmount.toPesoAmount()} of your " +
                "${alert.budgetAmount.toPesoAmount()} budget ($utilization%). " +
                "Over by ${(alert.spentAmount - alert.budgetAmount).toPesoAmount()}."
        } else {
            "You've used $utilization% of your $categoryName budget " +
                "(${alert.spentAmount.toPesoAmount()} of " +
                "${alert.budgetAmount.toPesoAmount()}). " +
                "${(alert.budgetAmount - alert.spentAmount).toPesoAmount()} remaining."
        }

        notificationManager.showBudgetAlertNotification(
            budgetId = alert.budgetId,
            threshold = alert.threshold,
            title = title,
            message = message
        )
    }
}
