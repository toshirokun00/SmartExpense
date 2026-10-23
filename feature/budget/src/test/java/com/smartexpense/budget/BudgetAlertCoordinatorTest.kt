package com.smartexpense.budget

import com.smartexpense.budget.alerts.BudgetAlertCoordinator
import com.smartexpense.domain.model.AppSettings
import com.smartexpense.domain.model.Budget
import com.smartexpense.domain.model.BudgetAlertRecord
import com.smartexpense.domain.model.Category
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.repository.BudgetAlertRepository
import com.smartexpense.domain.usecase.BudgetUseCase
import com.smartexpense.domain.usecase.CategoryUseCase
import com.smartexpense.domain.usecase.EvaluateBudgetAlertsUseCase
import com.smartexpense.domain.usecase.GetExpenseUseCase
import com.smartexpense.domain.usecase.SettingsUseCase
import com.smartexpense.notification.ExpenseNotificationManager
import com.smartexpense.test.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetAlertCoordinatorTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var getExpenseUseCase: GetExpenseUseCase
    private lateinit var budgetUseCase: BudgetUseCase
    private lateinit var categoryUseCase: CategoryUseCase
    private lateinit var settingsUseCase: SettingsUseCase
    private lateinit var budgetAlertRepository: BudgetAlertRepository
    private lateinit var notificationManager: ExpenseNotificationManager

    private lateinit var coordinator: BudgetAlertCoordinator

    private val start = dateMillis(2026, 10, 1)
    private val end = dateMillis(2026, 10, 31)

    private val budget = Budget(
        id = 1L,
        categoryId = 1L,
        amount = 1_000.0,
        startDate = start,
        endDate = end
    )

    private val categories = listOf(Category(1L, "Food"))

    @Before
    fun setup() {
        getExpenseUseCase = mockk()
        budgetUseCase = mockk()
        categoryUseCase = mockk()
        settingsUseCase = mockk()
        budgetAlertRepository = mockk()
        notificationManager = mockk()

        every {
            notificationManager.showBudgetAlertNotification(
                any(), any(), any(), any()
            )
        } returns true

        coEvery { budgetAlertRepository.recordDeliveredAlerts(any()) } just Runs
        coEvery { budgetAlertRepository.removeDeliveredAlerts(any()) } just Runs

        coordinator = BudgetAlertCoordinator(
            getExpenseUseCase = getExpenseUseCase,
            budgetUseCase = budgetUseCase,
            categoryUseCase = categoryUseCase,
            settingsUseCase = settingsUseCase,
            evaluateBudgetAlertsUseCase = EvaluateBudgetAlertsUseCase(),
            budgetAlertRepository = budgetAlertRepository,
            notificationManager = notificationManager
        )
    }

    private fun stubFlows(
        expenses: List<Expense>,
        delivered: List<BudgetAlertRecord> = emptyList(),
        settings: AppSettings = AppSettings(
            notificationsEnabled = true,
            budgetAlertThresholds = setOf(80, 100)
        )
    ) {
        every { budgetUseCase.getBudgets() } returns flowOf(listOf(budget))
        every { getExpenseUseCase() } returns flowOf(expenses)
        every { categoryUseCase.getCategories() } returns flowOf(categories)
        every { settingsUseCase.getSettings() } returns flowOf(settings)
        every { budgetAlertRepository.getDeliveredAlerts() } returns flowOf(delivered)
    }

    @Test
    fun crossingThresholdNotifiesAndRecordsDelivery() = runTest {
        stubFlows(
            expenses = listOf(Expense(1L, 850.0, 1L, "food", dateMillis(2026, 10, 10)))
        )

        coordinator.start(this)
        advanceUntilIdle()

        verify(exactly = 1) {
            notificationManager.showBudgetAlertNotification(
                budgetId = 1L,
                threshold = 80,
                title = any(),
                message = any()
            )
        }

        coVerify(exactly = 1) {
            budgetAlertRepository.recordDeliveredAlerts(
                listOf(BudgetAlertRecord(1L, start, end, 80))
            )
        }
    }

    @Test
    fun alreadyDeliveredThresholdDoesNotNotifyAgain() = runTest {
        stubFlows(
            expenses = listOf(Expense(1L, 850.0, 1L, "food", dateMillis(2026, 10, 10))),
            delivered = listOf(BudgetAlertRecord(1L, start, end, 80))
        )

        coordinator.start(this)
        advanceUntilIdle()

        verify(exactly = 0) {
            notificationManager.showBudgetAlertNotification(
                any(), any(), any(), any()
            )
        }
        coVerify(exactly = 0) { budgetAlertRepository.recordDeliveredAlerts(any()) }
    }

    @Test
    fun disabledNotificationsSkipDeliveryAndPersistence() = runTest {
        stubFlows(
            expenses = listOf(Expense(1L, 1_200.0, 1L, "food", dateMillis(2026, 10, 10))),
            settings = AppSettings(
                notificationsEnabled = false,
                budgetAlertThresholds = setOf(80, 100)
            )
        )

        coordinator.start(this)
        advanceUntilIdle()

        verify(exactly = 0) {
            notificationManager.showBudgetAlertNotification(
                any(), any(), any(), any()
            )
        }
        coVerify(exactly = 0) { budgetAlertRepository.recordDeliveredAlerts(any()) }
    }

    @Test
    fun droppingBelowThresholdRemovesDeliveryRecord() = runTest {
        stubFlows(
            expenses = listOf(Expense(1L, 200.0, 1L, "food", dateMillis(2026, 10, 10))),
            delivered = listOf(BudgetAlertRecord(1L, start, end, 80))
        )

        coordinator.start(this)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            budgetAlertRepository.removeDeliveredAlerts(
                listOf(BudgetAlertRecord(1L, start, end, 80))
            )
        }
        verify(exactly = 0) {
            notificationManager.showBudgetAlertNotification(
                any(), any(), any(), any()
            )
        }
    }

    @Test
    fun exceededBudgetEmitsThresholdNotificationAtOneHundred() = runTest {
        stubFlows(
            expenses = listOf(Expense(1L, 1_200.0, 1L, "food", dateMillis(2026, 10, 10)))
        )

        coordinator.start(this)
        advanceUntilIdle()

        verify(exactly = 1) {
            notificationManager.showBudgetAlertNotification(
                budgetId = 1L,
                threshold = 100,
                title = any(),
                message = any()
            )
        }
    }

    @Test
    fun noConfiguredThresholdsDeliverNoAlerts() = runTest {
        stubFlows(
            expenses = listOf(Expense(1L, 1_200.0, 1L, "food", dateMillis(2026, 10, 10))),
            settings = AppSettings(
                notificationsEnabled = true,
                budgetAlertThresholds = emptySet()
            )
        )

        coordinator.start(this)
        advanceUntilIdle()

        verify(exactly = 0) {
            notificationManager.showBudgetAlertNotification(
                any(), any(), any(), any()
            )
        }
        coVerify(exactly = 0) {
            budgetAlertRepository.recordDeliveredAlerts(any())
        }
    }

    private fun dateMillis(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
}
