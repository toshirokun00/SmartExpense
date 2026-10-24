package com.smartexpense.recurring

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartexpense.domain.model.Category
import com.smartexpense.domain.model.RecurrenceFrequency
import com.smartexpense.domain.model.RecurringExpenseSchedule
import com.smartexpense.domain.usecase.CategoryUseCase
import com.smartexpense.domain.usecase.ProcessRecurringExpensesUseCase
import com.smartexpense.domain.usecase.RecurringExpenseUseCase
import com.smartexpense.recurring.screen.RecurringExpenseFormScreen
import com.smartexpense.recurring.screen.RecurringExpenseListScreen
import com.smartexpense.recurring.viewmodel.RecurringExpenseFormViewModel
import com.smartexpense.recurring.viewmodel.RecurringExpenseListViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class RecurringExpenseScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val zone = ZoneId.systemDefault()

    @Test
    fun emptyStateIsShownWithoutSchedules() {
        val viewModel = listViewModel(
            recurringUseCase = relaxedRecurringUseCase(emptyList())
        )

        composeRule.setContent {
            RecurringExpenseListScreen(
                viewModel = viewModel,
                onNavigateBack = {},
                onAddSchedule = {},
                onEditSchedule = {}
            )
        }

        awaitText("No recurring expenses yet.")
        composeRule
            .onNodeWithText("No recurring expenses yet.")
            .assertIsDisplayed()
    }

    @Test
    fun scheduleCardShowsDetailsAndActions() {
        val viewModel = listViewModel(
            recurringUseCase = relaxedRecurringUseCase(
                listOf(schedule())
            )
        )

        composeRule.setContent {
            RecurringExpenseListScreen(
                viewModel = viewModel,
                onNavigateBack = {},
                onAddSchedule = {},
                onEditSchedule = {}
            )
        }

        awaitText("Netflix")

        composeRule.onNodeWithText("Netflix").assertIsDisplayed()
        composeRule.onNodeWithText("Food • Monthly").assertIsDisplayed()
        composeRule.onNodeWithText("Active").assertIsDisplayed()
        composeRule
            .onNodeWithText("Next:", substring = true)
            .assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Pause recurring expense")
            .assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Edit recurring expense")
            .assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Delete recurring expense")
            .assertIsDisplayed()
    }

    @Test
    fun pauseActionPausesScheduleAndShowsConfirmation() {
        val recurringUseCase = relaxedRecurringUseCase(
            listOf(schedule())
        )
        coEvery {
            recurringUseCase.pauseSchedule(1L)
        } returns Unit

        val viewModel = listViewModel(recurringUseCase)

        composeRule.setContent {
            RecurringExpenseListScreen(
                viewModel = viewModel,
                onNavigateBack = {},
                onAddSchedule = {},
                onEditSchedule = {}
            )
        }

        awaitText("Netflix")

        composeRule
            .onNodeWithContentDescription("Pause recurring expense")
            .performClick()

        awaitText("Schedule paused")
        composeRule.onNodeWithText("Schedule paused").assertIsDisplayed()

        coVerify(exactly = 1) {
            recurringUseCase.pauseSchedule(1L)
        }
    }

    @Test
    fun pausedScheduleOffersResume() {
        val recurringUseCase = relaxedRecurringUseCase(
            listOf(schedule(isActive = false))
        )
        coEvery {
            recurringUseCase.resumeSchedule(1L)
        } returns Unit

        val viewModel = listViewModel(recurringUseCase)

        composeRule.setContent {
            RecurringExpenseListScreen(
                viewModel = viewModel,
                onNavigateBack = {},
                onAddSchedule = {},
                onEditSchedule = {}
            )
        }

        awaitText("Paused")

        composeRule.onNodeWithText("Paused").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Resume recurring expense")
            .performClick()

        awaitText("Schedule resumed")
        composeRule.onNodeWithText("Schedule resumed").assertIsDisplayed()

        coVerify(exactly = 1) {
            recurringUseCase.resumeSchedule(1L)
        }
    }

    @Test
    fun deleteRequiresConfirmationBeforeCallingRepository() {
        val recurringUseCase = relaxedRecurringUseCase(
            listOf(schedule())
        )
        coEvery {
            recurringUseCase.deleteSchedule(1L)
        } returns Unit

        val viewModel = listViewModel(recurringUseCase)

        composeRule.setContent {
            RecurringExpenseListScreen(
                viewModel = viewModel,
                onNavigateBack = {},
                onAddSchedule = {},
                onEditSchedule = {}
            )
        }

        awaitText("Netflix")

        composeRule
            .onNodeWithContentDescription("Delete recurring expense")
            .performClick()

        awaitText("Delete recurring expense?")

        composeRule
            .onNodeWithText("Delete recurring expense?")
            .assertIsDisplayed()
        coVerify(exactly = 0) {
            recurringUseCase.deleteSchedule(any())
        }

        composeRule.onNodeWithText("Delete").performClick()

        awaitText("Schedule deleted")

        coVerify(exactly = 1) {
            recurringUseCase.deleteSchedule(1L)
        }
    }

    @Test
    fun formShowsValidationErrorForInvalidAmount() {
        val viewModel = formViewModel(
            recurringUseCase = mockk(relaxed = true)
        )

        composeRule.setContent {
            RecurringExpenseFormScreen(
                viewModel = viewModel,
                scheduleId = null,
                onNavigateBack = {},
                onSaved = {}
            )
        }

        awaitText("Create Schedule")

        composeRule
            .onNode(hasScrollToNodeAction())
            .performScrollToNode(
                hasText("Create Schedule") and hasClickAction()
            )

        composeRule
            .onNode(hasText("Create Schedule") and hasClickAction())
            .performClick()

        awaitText("Please enter a valid amount")
        composeRule
            .onNodeWithText("Please enter a valid amount")
            .assertIsDisplayed()
    }

    @Test
    fun editFormPrefillsExistingSchedule() {
        val existing = schedule()

        val recurringUseCase = mockk<RecurringExpenseUseCase>(relaxed = true)
        every {
            recurringUseCase.getSchedules()
        } returns flowOf(listOf(existing))
        coEvery {
            recurringUseCase.getScheduleById(1L)
        } returns existing

        val viewModel = formViewModel(recurringUseCase)

        composeRule.setContent {
            RecurringExpenseFormScreen(
                viewModel = viewModel,
                scheduleId = 1L,
                onNavigateBack = {},
                onSaved = {}
            )
        }

        awaitText("Edit Recurring Expense")

        composeRule
            .onNodeWithText("Edit Recurring Expense")
            .assertIsDisplayed()
        awaitText("Netflix")
        composeRule.onNodeWithText("Netflix").assertIsDisplayed()
        composeRule
            .onNodeWithText("Food")
            .assertIsDisplayed()
    }

    private fun listViewModel(
        recurringUseCase: RecurringExpenseUseCase
    ): RecurringExpenseListViewModel {
        val categoryUseCase = mockk<CategoryUseCase>()
        every {
            categoryUseCase.getCategories()
        } returns flowOf(categories())

        val processRecurringExpensesUseCase =
            mockk<ProcessRecurringExpensesUseCase>()
        coEvery {
            processRecurringExpensesUseCase()
        } returns 0

        return RecurringExpenseListViewModel(
            recurringExpenseUseCase = recurringUseCase,
            categoryUseCase = categoryUseCase,
            processRecurringExpensesUseCase =
                processRecurringExpensesUseCase
        )
    }

    private fun formViewModel(
        recurringUseCase: RecurringExpenseUseCase
    ): RecurringExpenseFormViewModel {
        val categoryUseCase = mockk<CategoryUseCase>()
        every {
            categoryUseCase.getCategories()
        } returns flowOf(categories())

        return RecurringExpenseFormViewModel(
            recurringExpenseUseCase = recurringUseCase,
            categoryUseCase = categoryUseCase,
            clock = Clock.fixed(
                Instant.parse("2026-02-15T12:00:00Z"),
                zone
            )
        )
    }

    private fun relaxedRecurringUseCase(
        schedules: List<RecurringExpenseSchedule>
    ): RecurringExpenseUseCase {
        val useCase = mockk<RecurringExpenseUseCase>(relaxed = true)
        every {
            useCase.getSchedules()
        } returns flowOf(schedules)
        return useCase
    }

    private fun categories(): List<Category> {
        return listOf(
            Category(id = 1L, name = "Food")
        )
    }

    private fun schedule(
        isActive: Boolean = true
    ): RecurringExpenseSchedule {
        return RecurringExpenseSchedule(
            id = 1L,
            clientId = "schedule-client-id",
            userId = "user-1",
            description = "Netflix",
            amount = 500.0,
            categoryId = 1L,
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = dateMillis(2026, 1, 1),
            nextOccurrence = dateMillis(2026, 3, 1),
            isActive = isActive
        )
    }

    private fun dateMillis(
        year: Int,
        month: Int,
        day: Int
    ): Long {
        return LocalDate.of(year, month, day)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
    }

    private fun awaitText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithText(text, substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }
}
