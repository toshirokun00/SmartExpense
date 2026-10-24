package com.smartexpense.recurring

import app.cash.turbine.test
import com.smartexpense.domain.model.Category
import com.smartexpense.domain.model.RecurrenceFrequency
import com.smartexpense.domain.model.RecurringExpenseSchedule
import com.smartexpense.domain.model.RecurringScheduleStatus
import com.smartexpense.domain.recurring.RecurringDateUtils
import com.smartexpense.domain.usecase.CategoryUseCase
import com.smartexpense.domain.usecase.ProcessRecurringExpensesUseCase
import com.smartexpense.domain.usecase.RecurringExpenseUseCase
import com.smartexpense.recurring.effect.RecurringExpenseEffect
import com.smartexpense.recurring.intent.RecurringExpenseIntent
import com.smartexpense.recurring.viewmodel.RecurringExpenseListViewModel
import com.smartexpense.test.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class RecurringExpenseListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val zone = ZoneId.of("UTC")
    private val userId = "user-1"

    private val categories = listOf(
        Category(id = 1L, name = "Food"),
        Category(id = 2L, name = "Rent")
    )

    @Test
    fun `loads schedules with category names labels and status`() =
        runTest {
            val recurringUseCase = recurringUseCase(
                flowOf(
                    listOf(
                        schedule(
                            id = 1L,
                            description = "Netflix",
                            categoryId = 1L,
                            frequency = RecurrenceFrequency.MONTHLY,
                            next = "2026-03-01"
                        ),
                        schedule(
                            id = 2L,
                            description = "Rent",
                            categoryId = 2L,
                            frequency = RecurrenceFrequency.MONTHLY,
                            next = "2026-03-01",
                            isActive = false
                        )
                    )
                )
            )

            val viewModel = createViewModel(recurringUseCase)

            viewModel.uiState.test {
                assertEquals(true, awaitItem().isLoading)

                val state = awaitItem()

                assertFalse(state.isLoading)
                assertEquals(2, state.schedules.size)
                assertEquals("Food", state.schedules[0].categoryName)
                assertEquals("Monthly", state.schedules[0].frequencyLabel)
                assertEquals(
                    RecurringScheduleStatus.ACTIVE,
                    state.schedules[0].status
                )
                assertEquals(
                    RecurringScheduleStatus.PAUSED,
                    state.schedules[1].status
                )
                assertNull(state.errorMessage)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `load failure produces an error state`() = runTest {
        val recurringUseCase = recurringUseCase(
            flow {
                throw IllegalStateException("database unavailable")
            }
        )

        val viewModel = createViewModel(recurringUseCase)

        viewModel.uiState.test {
            awaitItem()

            val state = awaitItem()

            assertFalse(state.isLoading)
            assertEquals("database unavailable", state.errorMessage)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pause intent pauses the schedule and emits confirmation`() =
        runTest {
            val recurringUseCase = recurringUseCase(flowOf(emptyList()))
            coEvery {
                recurringUseCase.pauseSchedule(1L)
            } returns Unit

            val viewModel = createViewModel(recurringUseCase)

            viewModel.effect.test {
                viewModel.onIntent(RecurringExpenseIntent.Pause(1L))

                assertEquals(
                    RecurringExpenseEffect.SchedulePaused,
                    awaitItem()
                )

                cancelAndIgnoreRemainingEvents()
            }

            coVerify(exactly = 1) {
                recurringUseCase.pauseSchedule(1L)
            }
        }

    @Test
    fun `resume intent resumes the schedule and emits confirmation`() =
        runTest {
            val recurringUseCase = recurringUseCase(flowOf(emptyList()))
            coEvery {
                recurringUseCase.resumeSchedule(1L)
            } returns Unit

            val viewModel = createViewModel(recurringUseCase)

            viewModel.effect.test {
                viewModel.onIntent(RecurringExpenseIntent.Resume(1L))

                assertEquals(
                    RecurringExpenseEffect.ScheduleResumed,
                    awaitItem()
                )

                cancelAndIgnoreRemainingEvents()
            }

            coVerify(exactly = 1) {
                recurringUseCase.resumeSchedule(1L)
            }
        }

    @Test
    fun `delete requires confirmation before deleting`() = runTest {
        val recurringUseCase = recurringUseCase(flowOf(emptyList()))
        coEvery {
            recurringUseCase.deleteSchedule(1L)
        } returns Unit

        val viewModel = createViewModel(recurringUseCase)

        viewModel.effect.test {
            viewModel.onIntent(RecurringExpenseIntent.RequestDelete(1L))

            assertEquals(
                1L,
                viewModel.screenState.value.scheduleToDeleteId
            )
            assertTrue(viewModel.screenState.value.showDeleteConfirmation)

            viewModel.onIntent(RecurringExpenseIntent.ConfirmDelete)

            assertEquals(
                RecurringExpenseEffect.ScheduleDeleted,
                awaitItem()
            )
        }

        coVerify(exactly = 1) {
            recurringUseCase.deleteSchedule(1L)
        }
        assertFalse(viewModel.screenState.value.showDeleteConfirmation)
        assertNull(viewModel.screenState.value.scheduleToDeleteId)
    }

    @Test
    fun `delete can be cancelled`() = runTest {
        val recurringUseCase = recurringUseCase(flowOf(emptyList()))
        val viewModel = createViewModel(recurringUseCase)

        viewModel.onIntent(RecurringExpenseIntent.RequestDelete(1L))
        viewModel.onIntent(RecurringExpenseIntent.CancelDelete)

        assertFalse(viewModel.screenState.value.showDeleteConfirmation)
        assertNull(viewModel.screenState.value.scheduleToDeleteId)
        coVerify(exactly = 0) {
            recurringUseCase.deleteSchedule(any())
        }
    }

    @Test
    fun `action failure is surfaced as an error`() = runTest {
        val recurringUseCase = recurringUseCase(flowOf(emptyList()))
        coEvery {
            recurringUseCase.pauseSchedule(1L)
        } throws IllegalStateException("not found")

        val viewModel = createViewModel(recurringUseCase)

        viewModel.effect.test {
            viewModel.onIntent(RecurringExpenseIntent.Pause(1L))

            assertEquals(
                RecurringExpenseEffect.ShowError("not found"),
                awaitItem()
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createViewModel(
        recurringUseCase: RecurringExpenseUseCase
    ): RecurringExpenseListViewModel {
        val categoryUseCase = mockk<CategoryUseCase>()
        every {
            categoryUseCase.getCategories()
        } returns flowOf(categories)

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

    private fun recurringUseCase(
        schedules: kotlinx.coroutines.flow.Flow<List<RecurringExpenseSchedule>>
    ): RecurringExpenseUseCase {
        val useCase = mockk<RecurringExpenseUseCase>()
        every {
            useCase.getSchedules()
        } returns schedules
        return useCase
    }

    private fun schedule(
        id: Long,
        description: String,
        categoryId: Long,
        frequency: RecurrenceFrequency,
        next: String,
        isActive: Boolean = true
    ): RecurringExpenseSchedule {
        return RecurringExpenseSchedule(
            id = id,
            clientId = "schedule-$id",
            userId = userId,
            description = description,
            amount = 500.0,
            categoryId = categoryId,
            frequency = frequency,
            startDate = millis("2026-01-01"),
            nextOccurrence = millis(next),
            isActive = isActive
        )
    }

    private fun millis(value: String): Long {
        return RecurringDateUtils.toEpochMillis(
            date = LocalDate.parse(value),
            zoneId = zone
        )
    }
}
