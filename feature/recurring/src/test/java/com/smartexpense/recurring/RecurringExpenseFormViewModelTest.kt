package com.smartexpense.recurring

import app.cash.turbine.test
import com.smartexpense.domain.model.Category
import com.smartexpense.domain.model.RecurrenceFrequency
import com.smartexpense.domain.model.RecurringExpenseSchedule
import com.smartexpense.domain.recurring.RecurringDateUtils
import com.smartexpense.domain.usecase.CategoryUseCase
import com.smartexpense.domain.usecase.RecurringExpenseUseCase
import com.smartexpense.recurring.effect.RecurringExpenseFormEffect
import com.smartexpense.recurring.intent.RecurringExpenseFormIntent
import com.smartexpense.recurring.viewmodel.RecurringExpenseFormViewModel
import com.smartexpense.test.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class RecurringExpenseFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val zone = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 2, 15)
    private val clock = Clock.fixed(
        Instant.parse("2026-02-15T12:00:00Z"),
        zone
    )

    private val categories = listOf(
        Category(id = 1L, name = "Food")
    )

    @Test
    fun `new form defaults to today and monthly frequency`() {
        val viewModel = createViewModel(
            recurringUseCase = mockk()
        )

        assertEquals(
            millis(today),
            viewModel.formState.value.startDate
        )
        assertEquals(
            RecurrenceFrequency.MONTHLY,
            viewModel.formState.value.frequency
        )
        assertEquals("1", viewModel.formState.value.interval)
        assertFalse(viewModel.formState.value.isSaving)
    }

    @Test
    fun `invalid amount shows validation error`() = runTest {
        val viewModel = createViewModel(mockk())

        viewModel.effect.test {
            viewModel.onIntent(RecurringExpenseFormIntent.Save)

            assertEquals(
                RecurringExpenseFormEffect.ShowError(
                    "Please enter a valid amount"
                ),
                awaitItem()
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `non numeric amount shows validation error`() = runTest {
        val viewModel = createViewModel(mockk())

        viewModel.onIntent(RecurringExpenseFormIntent.AmountChanged("abc"))

        viewModel.effect.test {
            viewModel.onIntent(RecurringExpenseFormIntent.Save)

            assertEquals(
                RecurringExpenseFormEffect.ShowError(
                    "Please enter a valid amount"
                ),
                awaitItem()
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `missing category shows validation error`() = runTest {
        val viewModel = createViewModel(mockk())

        viewModel.onIntent(RecurringExpenseFormIntent.AmountChanged("1000"))

        viewModel.effect.test {
            viewModel.onIntent(RecurringExpenseFormIntent.Save)

            assertEquals(
                RecurringExpenseFormEffect.ShowError(
                    "Please select a category"
                ),
                awaitItem()
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `blank description shows validation error`() = runTest {
        val viewModel = createViewModel(mockk())
        fillAmountAndCategory(viewModel)

        viewModel.effect.test {
            viewModel.onIntent(RecurringExpenseFormIntent.Save)

            assertEquals(
                RecurringExpenseFormEffect.ShowError(
                    "Please enter a description"
                ),
                awaitItem()
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `interval below one shows validation error`() = runTest {
        val viewModel = createViewModel(mockk())
        fillValidForm(viewModel)
        viewModel.onIntent(RecurringExpenseFormIntent.IntervalChanged("0"))

        viewModel.effect.test {
            viewModel.onIntent(RecurringExpenseFormIntent.Save)

            assertEquals(
                RecurringExpenseFormEffect.ShowError(
                    "Repeat interval must be at least 1"
                ),
                awaitItem()
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `end date before start date shows validation error`() = runTest {
        val viewModel = createViewModel(mockk())
        fillValidForm(viewModel)
        viewModel.onIntent(
            RecurringExpenseFormIntent.EndDateChanged(
                millis(today.minusDays(1))
            )
        )

        viewModel.effect.test {
            viewModel.onIntent(RecurringExpenseFormIntent.Save)

            assertEquals(
                RecurringExpenseFormEffect.ShowError(
                    "End date cannot be before start date"
                ),
                awaitItem()
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `valid form saves a schedule and emits saved`() = runTest {
        val recurringUseCase = mockk<RecurringExpenseUseCase>()
        val captured = slot<RecurringExpenseSchedule>()

        coEvery {
            recurringUseCase.addSchedule(capture(captured))
        } returns Unit

        val viewModel = createViewModel(recurringUseCase)
        fillValidForm(viewModel)
        viewModel.onIntent(RecurringExpenseFormIntent.IntervalChanged("2"))

        val clientId = viewModel.formState.value.clientId

        viewModel.effect.test {
            viewModel.onIntent(RecurringExpenseFormIntent.Save)

            assertEquals(
                RecurringExpenseFormEffect.Saved,
                awaitItem()
            )

            cancelAndIgnoreRemainingEvents()
        }

        assertEquals("Rent", captured.captured.description)
        assertEquals(1000.0, captured.captured.amount, 0.0)
        assertEquals(1L, captured.captured.categoryId)
        assertEquals(
            RecurrenceFrequency.MONTHLY,
            captured.captured.frequency
        )
        assertEquals(2, captured.captured.interval)
        assertEquals(millis(today), captured.captured.startDate)
        assertEquals(clientId, captured.captured.clientId)
        assertTrue(viewModel.formState.value.isSaving)

        coVerify(exactly = 1) {
            recurringUseCase.addSchedule(any())
        }
    }

    @Test
    fun `double submit only saves once`() = runTest {
        val recurringUseCase = mockk<RecurringExpenseUseCase>()

        coEvery {
            recurringUseCase.addSchedule(any())
        } coAnswers {
            delay(1_000)
        }

        val viewModel = createViewModel(recurringUseCase)
        fillValidForm(viewModel)

        viewModel.onIntent(RecurringExpenseFormIntent.Save)
        viewModel.onIntent(RecurringExpenseFormIntent.Save)

        assertTrue(viewModel.formState.value.isSaving)

        advanceUntilIdle()

        coVerify(exactly = 1) {
            recurringUseCase.addSchedule(any())
        }
    }

    @Test
    fun `save failure resets progress and shows error`() = runTest {
        val recurringUseCase = mockk<RecurringExpenseUseCase>()

        coEvery {
            recurringUseCase.addSchedule(any())
        } throws IllegalStateException("Sign in to manage recurring expenses")

        val viewModel = createViewModel(recurringUseCase)
        fillValidForm(viewModel)

        viewModel.effect.test {
            viewModel.onIntent(RecurringExpenseFormIntent.Save)

            assertEquals(
                RecurringExpenseFormEffect.ShowError(
                    "Sign in to manage recurring expenses"
                ),
                awaitItem()
            )

            cancelAndIgnoreRemainingEvents()
        }

        assertFalse(viewModel.formState.value.isSaving)
    }

    @Test
    fun `loading an existing schedule fills the form`() = runTest {
        val existing = schedule(
            id = 7L,
            description = "Rent",
            amount = 1200.0,
            categoryId = 1L,
            interval = 1,
            start = "2026-01-01",
            next = "2026-03-01"
        )

        val recurringUseCase = mockk<RecurringExpenseUseCase>()

        coEvery {
            recurringUseCase.getScheduleById(7L)
        } returns existing

        val viewModel = createViewModel(recurringUseCase)
        viewModel.onIntent(RecurringExpenseFormIntent.LoadSchedule(7L))
        advanceUntilIdle()

        val form = viewModel.formState.value

        assertEquals(7L, form.editingScheduleId)
        assertEquals(existing.clientId, form.clientId)
        assertEquals("Rent", form.description)
        assertEquals("1200.0", form.amount)
        assertEquals(1L, form.categoryId)
        assertEquals(RecurrenceFrequency.MONTHLY, form.frequency)
        assertEquals("1", form.interval)
        assertEquals(existing.startDate, form.startDate)
        assertFalse(form.isLoading)
    }

    @Test
    fun `editing an existing schedule calls update with the same client id`() =
        runTest {
            val existing = schedule(
                id = 7L,
                description = "Rent",
                amount = 1200.0,
                categoryId = 1L,
                interval = 1,
                start = "2026-01-01",
                next = "2026-01-01"
            )

            val recurringUseCase = mockk<RecurringExpenseUseCase>()
            val captured = slot<RecurringExpenseSchedule>()

            coEvery {
                recurringUseCase.getScheduleById(7L)
            } returns existing

            coEvery {
                recurringUseCase.updateSchedule(capture(captured))
            } returns Unit

            val viewModel = createViewModel(recurringUseCase)
            viewModel.onIntent(RecurringExpenseFormIntent.LoadSchedule(7L))
            advanceUntilIdle()

            viewModel.onIntent(
                RecurringExpenseFormIntent.AmountChanged("1500")
            )

            viewModel.effect.test {
                viewModel.onIntent(RecurringExpenseFormIntent.Save)

                assertEquals(
                    RecurringExpenseFormEffect.Saved,
                    awaitItem()
                )

                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(7L, captured.captured.id)
            assertEquals(existing.clientId, captured.captured.clientId)
            assertEquals(1500.0, captured.captured.amount, 0.0)

            coVerify(exactly = 1) {
                recurringUseCase.updateSchedule(any())
            }
            coVerify(exactly = 0) {
                recurringUseCase.addSchedule(any())
            }
        }

    @Test
    fun `missing schedule reports an error and stops loading`() = runTest {
        val recurringUseCase = mockk<RecurringExpenseUseCase>()

        coEvery {
            recurringUseCase.getScheduleById(7L)
        } returns null

        val viewModel = createViewModel(recurringUseCase)

        viewModel.effect.test {
            viewModel.onIntent(RecurringExpenseFormIntent.LoadSchedule(7L))
            advanceUntilIdle()

            assertEquals(
                RecurringExpenseFormEffect.ShowError(
                    "Recurring expense not found"
                ),
                awaitItem()
            )

            cancelAndIgnoreRemainingEvents()
        }

        assertFalse(viewModel.formState.value.isLoading)
    }

    private fun createViewModel(
        recurringUseCase: RecurringExpenseUseCase
    ): RecurringExpenseFormViewModel {
        val categoryUseCase = mockk<CategoryUseCase>()
        every {
            categoryUseCase.getCategories()
        } returns flowOf(categories)

        return RecurringExpenseFormViewModel(
            recurringExpenseUseCase = recurringUseCase,
            categoryUseCase = categoryUseCase,
            clock = clock
        )
    }

    private fun fillAmountAndCategory(
        viewModel: RecurringExpenseFormViewModel
    ) {
        viewModel.onIntent(RecurringExpenseFormIntent.AmountChanged("1000"))
        viewModel.onIntent(RecurringExpenseFormIntent.CategoryChanged(1L))
    }

    private fun fillValidForm(
        viewModel: RecurringExpenseFormViewModel
    ) {
        viewModel.onIntent(
            RecurringExpenseFormIntent.DescriptionChanged("Rent")
        )
        fillAmountAndCategory(viewModel)
    }

    private fun schedule(
        id: Long,
        description: String,
        amount: Double,
        categoryId: Long,
        interval: Int,
        start: String,
        next: String
    ): RecurringExpenseSchedule {
        return RecurringExpenseSchedule(
            id = id,
            clientId = "existing-client-id",
            userId = "user-1",
            description = description,
            amount = amount,
            categoryId = categoryId,
            frequency = RecurrenceFrequency.MONTHLY,
            interval = interval,
            startDate = millis(LocalDate.parse(start)),
            endDate = null,
            nextOccurrence = millis(LocalDate.parse(next)),
            isActive = true
        )
    }

    private fun millis(date: LocalDate): Long {
        return RecurringDateUtils.toEpochMillis(
            date = date,
            zoneId = zone
        )
    }
}
