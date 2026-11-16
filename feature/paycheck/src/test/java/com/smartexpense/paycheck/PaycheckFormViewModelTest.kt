package com.smartexpense.paycheck

import app.cash.turbine.test
import com.smartexpense.domain.model.Bill
import com.smartexpense.domain.model.BillFrequency
import com.smartexpense.domain.model.BillPayment
import com.smartexpense.domain.model.Paycheck
import com.smartexpense.domain.model.PaycheckFrequency
import com.smartexpense.domain.model.isOccurrencePaid
import com.smartexpense.domain.model.planPaychecks
import com.smartexpense.domain.usecase.AddPaycheckScheduleUseCase
import com.smartexpense.domain.usecase.AddPaycheckUseCase
import com.smartexpense.domain.usecase.DeletePaycheckUseCase
import com.smartexpense.domain.usecase.GetBillPaymentHistoryUseCase
import com.smartexpense.domain.usecase.GetBillsUseCase
import com.smartexpense.domain.usecase.GetPaycheckUseCase
import com.smartexpense.domain.usecase.GetPaychecksUseCase
import com.smartexpense.domain.usecase.StopPaycheckScheduleUseCase
import com.smartexpense.domain.usecase.UpdatePaycheckUseCase
import com.smartexpense.paycheck.effect.PaycheckFormEffect
import com.smartexpense.paycheck.intent.PaycheckFormIntent
import com.smartexpense.paycheck.state.PaycheckFormState
import com.smartexpense.paycheck.state.PaycheckFormUiState
import com.smartexpense.paycheck.viewmodel.PaycheckFormViewModel
import com.smartexpense.test.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class PaycheckFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var getPaycheckUseCase: GetPaycheckUseCase
    private lateinit var addPaycheckUseCase: AddPaycheckUseCase
    private lateinit var updatePaycheckUseCase: UpdatePaycheckUseCase
    private lateinit var deletePaycheckUseCase: DeletePaycheckUseCase
    private lateinit var addPaycheckScheduleUseCase: AddPaycheckScheduleUseCase
    private lateinit var stopPaycheckScheduleUseCase: StopPaycheckScheduleUseCase
    private lateinit var getBillsUseCase: GetBillsUseCase
    private lateinit var getPaychecksUseCase: GetPaychecksUseCase
    private lateinit var getBillPaymentHistoryUseCase: GetBillPaymentHistoryUseCase

    private val clock = Clock.fixed(
        Instant.parse("2026-11-10T12:00:00Z"),
        ZoneOffset.UTC
    )

    private val bills = listOf(
        bill(id = 1, day = 5, amount = 500.0),
        bill(id = 2, day = 10, amount = 300.0),
        bill(id = 3, day = 20, amount = 200.0)
    )

    @Before
    fun setup() {
        getPaycheckUseCase = mockk()
        addPaycheckUseCase = mockk()
        updatePaycheckUseCase = mockk()
        deletePaycheckUseCase = mockk()
        addPaycheckScheduleUseCase = mockk()
        stopPaycheckScheduleUseCase = mockk()
        getBillsUseCase = mockk()
        getPaychecksUseCase = mockk()
        getBillPaymentHistoryUseCase = mockk()

        every { getBillsUseCase() } returns flowOf(bills)
        every { getPaychecksUseCase() } returns flowOf(emptyList())
        every { getBillPaymentHistoryUseCase() } returns flowOf(emptyList())
        coEvery { addPaycheckUseCase(any()) } returns 1L
        coEvery { updatePaycheckUseCase(any()) } just Runs
        coEvery { deletePaycheckUseCase(any()) } just Runs
        coEvery { addPaycheckScheduleUseCase(any(), any()) } returns 1L
        coEvery { stopPaycheckScheduleUseCase(any()) } just Runs
    }

    private fun createViewModel(
        billsFlow: MutableStateFlow<List<Bill>>? = null,
        paychecksFlow: MutableStateFlow<List<Paycheck>>? = null
    ): PaycheckFormViewModel {
        if (billsFlow != null) {
            every { getBillsUseCase() } returns billsFlow
        }
        if (paychecksFlow != null) {
            every { getPaychecksUseCase() } returns paychecksFlow
        }
        return PaycheckFormViewModel(
            getPaycheckUseCase = getPaycheckUseCase,
            addPaycheckUseCase = addPaycheckUseCase,
            updatePaycheckUseCase = updatePaycheckUseCase,
            deletePaycheckUseCase = deletePaycheckUseCase,
            addPaycheckScheduleUseCase = addPaycheckScheduleUseCase,
            stopPaycheckScheduleUseCase = stopPaycheckScheduleUseCase,
            getBillsUseCase = getBillsUseCase,
            getPaychecksUseCase = getPaychecksUseCase,
            getBillPaymentHistoryUseCase = getBillPaymentHistoryUseCase,
            clock = clock
        )
    }

    private fun bill(id: Long, day: Int, amount: Double): Bill = Bill(
        id = id,
        name = "Bill $id",
        amount = amount,
        dueDateMillis = LocalDate.of(2026, 11, day)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    )

    /** What a draft paycheck at [payDateMillis] plans to cover. */
    private fun expectedSuggestions(
        payDateMillis: Long,
        source: List<Bill> = bills
    ): Set<Long> = planPaychecks(
        paychecks = listOf(
            Paycheck(id = -1L, amount = 0.0, payDateMillis = payDateMillis)
        ),
        bills = source,
        zoneId = ZoneOffset.UTC
    )[-1L]?.billIds.orEmpty().toSet()

    @Test
    fun `new paycheck preselects the bills in its coverage window`() = runTest {
        val viewModel = createViewModel()

        viewModel.load(null)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(
            expectedSuggestions(state.form.payDateMillis),
            state.form.selectedBillIds
        )
    }

    @Test
    fun `a one-time bill covered by another paycheck is not suggested`() = runTest {
        // Another paycheck on the 15th already covers bill 1.
        val paychecksFlow = MutableStateFlow(
            listOf(
                Paycheck(
                    id = 5L,
                    amount = 2_000.0,
                    payDateMillis = dateMillis(2026, 11, 15),
                    label = "Salary",
                    billIds = listOf(1L)
                )
            )
        )
        val viewModel = createViewModel(paychecksFlow = paychecksFlow)
        viewModel.load(null)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(setOf(1L), state.coveredBillIds)
        // The new paycheck sits before that one, so only the bills due on the
        // day it is created are planned for it.
        assertEquals(setOf(2L), state.form.selectedBillIds)
    }

    @Test
    fun `moving the pay date covers the bills in the new window`() = runTest {
        val paychecksFlow = MutableStateFlow(
            listOf(
                Paycheck(
                    id = 5L,
                    amount = 2_000.0,
                    payDateMillis = dateMillis(2026, 11, 15),
                    label = "Salary",
                    billIds = listOf(1L)
                )
            )
        )
        val viewModel = createViewModel(paychecksFlow = paychecksFlow)
        viewModel.load(null)
        advanceUntilIdle()

        val newPayDate = dateMillis(2026, 11, 20)
        viewModel.onIntent(PaycheckFormIntent.PayDateChanged(newPayDate))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // Bill 1 stays covered by the other paycheck; bill 2 falls before the
        // new pay date and bill 3 is now due on it.
        assertFalse(1L in state.form.selectedBillIds)
        assertFalse(2L in state.form.selectedBillIds)
        assertTrue(3L in state.form.selectedBillIds)
    }

    @Test
    fun `editing a paycheck does not treat its own bills as covered`() = runTest {
        coEvery { getPaycheckUseCase(9L) } returns Paycheck(
            id = 9L,
            amount = 2_000.0,
            payDateMillis = dateMillis(2026, 11, 15),
            label = "Salary",
            billIds = listOf(1L, 2L)
        )
        val paychecksFlow = MutableStateFlow(
            listOf(
                Paycheck(
                    id = 9L,
                    amount = 2_000.0,
                    payDateMillis = dateMillis(2026, 11, 15),
                    label = "Salary",
                    billIds = listOf(1L, 2L)
                ),
                Paycheck(
                    id = 5L,
                    amount = 2_000.0,
                    payDateMillis = dateMillis(2026, 11, 30),
                    label = "Salary",
                    billIds = listOf(3L)
                )
            )
        )
        val viewModel = createViewModel(paychecksFlow = paychecksFlow)

        viewModel.load(9L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(setOf(3L), state.coveredBillIds)
        // The saved selection wins and is untouched by the coverage rule.
        assertEquals(setOf(1L, 2L), state.form.selectedBillIds)
    }

    @Test
    fun `changing the pay date reselects suggestions`() = runTest {
        val viewModel = createViewModel()
        viewModel.load(null)
        advanceUntilIdle()

        val newPayDate = LocalDate.of(2026, 11, 7)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        viewModel.onIntent(PaycheckFormIntent.PayDateChanged(newPayDate))
        advanceUntilIdle()

        assertEquals(newPayDate, viewModel.uiState.value.form.payDateMillis)
        assertEquals(
            expectedSuggestions(newPayDate),
            viewModel.uiState.value.form.selectedBillIds
        )
    }

    @Test
    fun `manual selection is kept when bills update`() = runTest {
        val billsFlow = MutableStateFlow(bills)
        val viewModel = createViewModel(billsFlow)
        viewModel.load(null)
        advanceUntilIdle()

        viewModel.onIntent(PaycheckFormIntent.BillToggled(1L))
        advanceUntilIdle()
        val selectedAfterToggle = viewModel.uiState.value.form.selectedBillIds
        assertFalse(1L in selectedAfterToggle)

        billsFlow.value = bills + bill(id = 4, day = 1, amount = 50.0)
        advanceUntilIdle()

        assertEquals(
            selectedAfterToggle,
            viewModel.uiState.value.form.selectedBillIds
        )
    }

    @Test
    fun `save with invalid amount shows an error`() = runTest {
        val viewModel = createViewModel()
        viewModel.load(null)
        viewModel.onIntent(PaycheckFormIntent.AmountChanged("0"))

        viewModel.effect.test {
            viewModel.onIntent(PaycheckFormIntent.Save)

            assertEquals(
                PaycheckFormEffect.ShowError(
                    "Please enter a valid paycheck amount"
                ),
                awaitItem()
            )
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { addPaycheckUseCase(any()) }
    }

    @Test
    fun `save adds a paycheck with the selected bills`() = runTest {
        val viewModel = createViewModel()
        viewModel.load(null)
        advanceUntilIdle()
        viewModel.onIntent(PaycheckFormIntent.AmountChanged("1,500"))
        viewModel.onIntent(PaycheckFormIntent.LabelChanged("  1st salary "))
        val selected = viewModel.uiState.value.form.selectedBillIds

        viewModel.effect.test {
            viewModel.onIntent(PaycheckFormIntent.Save)
            advanceUntilIdle()

            assertEquals(PaycheckFormEffect.Saved, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) {
            addPaycheckUseCase(
                match {
                    it.amount == 1500.0 &&
                        it.label == "1st salary" &&
                        it.billIds.toSet() == selected
                }
            )
        }
    }

    @Test
    fun `editing loads the saved paycheck and its bills`() = runTest {
        coEvery { getPaycheckUseCase(9L) } returns Paycheck(
            id = 9L,
            amount = 2000.0,
            payDateMillis = LocalDate.of(2026, 11, 15)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli(),
            label = "2nd salary",
            billIds = listOf(2L, 3L)
        )
        val viewModel = createViewModel()

        viewModel.load(9L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2000.0, state.form.amount.clean(), 0.001)
        assertEquals("2nd salary", state.form.label)
        assertEquals(setOf(2L, 3L), state.form.selectedBillIds)
        assertEquals(9L, state.form.editingPaycheckId)
    }

    @Test
    fun `editing saves through the update use case`() = runTest {
        coEvery { getPaycheckUseCase(9L) } returns Paycheck(
            id = 9L,
            amount = 2000.0,
            payDateMillis = 0L,
            label = "Pay",
            billIds = listOf(2L)
        )
        val viewModel = createViewModel()
        viewModel.load(9L)
        advanceUntilIdle()
        viewModel.onIntent(PaycheckFormIntent.AmountChanged("2,500"))

        viewModel.onIntent(PaycheckFormIntent.Save)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            updatePaycheckUseCase(
                match { it.id == 9L && it.amount == 2500.0 }
            )
        }
        coVerify(exactly = 0) { addPaycheckUseCase(any()) }
    }

    @Test
    fun `delete removes the paycheck being edited`() = runTest {
        coEvery { getPaycheckUseCase(9L) } returns Paycheck(
            id = 9L,
            amount = 2000.0,
            payDateMillis = 0L
        )
        val viewModel = createViewModel()
        viewModel.load(9L)
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(PaycheckFormIntent.Delete)
            advanceUntilIdle()

            assertEquals(PaycheckFormEffect.Deleted, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { deletePaycheckUseCase(9L) }
    }

    @Test
    fun `saving a recurring paycheck creates a semi-monthly schedule`() = runTest {
        val viewModel = createViewModel()
        viewModel.load(null)
        advanceUntilIdle()
        val payDate = dateMillis(2026, 11, 15)
        viewModel.onIntent(PaycheckFormIntent.PayDateChanged(payDate))
        viewModel.onIntent(PaycheckFormIntent.RecurringChanged(true))
        viewModel.onIntent(PaycheckFormIntent.AmountChanged("2,000"))
        viewModel.onIntent(PaycheckFormIntent.LabelChanged(" Salary "))
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onIntent(PaycheckFormIntent.Save)
            advanceUntilIdle()

            assertEquals(PaycheckFormEffect.Saved, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { addPaycheckUseCase(any()) }
        coVerify(exactly = 1) {
            addPaycheckScheduleUseCase(
                match {
                    it.amount == 2000.0 &&
                        it.label == "Salary" &&
                        it.frequency == PaycheckFrequency.SEMI_MONTHLY &&
                        it.firstDay == 15 &&
                        it.secondDay == 30 &&
                        it.startDateMillis == payDate
                },
                match { it.toSet() == setOf(1L, 2L, 3L) }
            )
        }
    }

    @Test
    fun `enabling recurrence keeps the chosen pay date as the first paycheck`() = runTest {
        val viewModel = createViewModel()
        viewModel.load(null)
        advanceUntilIdle()
        val payDate = dateMillis(2026, 11, 20)
        viewModel.onIntent(PaycheckFormIntent.PayDateChanged(payDate))

        viewModel.onIntent(PaycheckFormIntent.RecurringChanged(true))
        advanceUntilIdle()

        val form = viewModel.uiState.value.form
        assertEquals(payDate, form.payDateMillis)
        assertFalse(form.canEditPayDate)
        assertEquals(20, form.firstDay)
        assertEquals(15, form.secondDay)
    }

    @Test
    fun `changing the recurrence rule previews the next cutoff`() = runTest {
        val viewModel = createViewModel()
        viewModel.load(null)
        advanceUntilIdle()
        viewModel.onIntent(PaycheckFormIntent.RecurringChanged(true))
        viewModel.onIntent(PaycheckFormIntent.FrequencyChanged(PaycheckFrequency.SEMI_MONTHLY))
        viewModel.onIntent(PaycheckFormIntent.FirstDayChanged(15))
        viewModel.onIntent(PaycheckFormIntent.SecondDayChanged(30))
        advanceUntilIdle()

        assertEquals(
            dateMillis(2026, 11, 15),
            viewModel.uiState.value.form.payDateMillis
        )
    }

    @Test
    fun `switching to weekly keeps the chosen schedule start`() = runTest {
        val viewModel = createViewModel()
        viewModel.load(null)
        advanceUntilIdle()
        val payDate = dateMillis(2026, 11, 20)
        viewModel.onIntent(PaycheckFormIntent.PayDateChanged(payDate))
        viewModel.onIntent(PaycheckFormIntent.RecurringChanged(true))
        viewModel.onIntent(PaycheckFormIntent.FrequencyChanged(PaycheckFrequency.WEEKLY))
        advanceUntilIdle()

        assertEquals(payDate, viewModel.uiState.value.form.payDateMillis)
    }

    @Test
    fun `monthly recurring schedules save a single pay day`() = runTest {
        val viewModel = createViewModel()
        viewModel.load(null)
        advanceUntilIdle()
        viewModel.onIntent(PaycheckFormIntent.RecurringChanged(true))
        viewModel.onIntent(PaycheckFormIntent.FrequencyChanged(PaycheckFrequency.MONTHLY))
        viewModel.onIntent(PaycheckFormIntent.FirstDayChanged(25))
        viewModel.onIntent(PaycheckFormIntent.AmountChanged("2,000"))
        advanceUntilIdle()

        viewModel.onIntent(PaycheckFormIntent.Save)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            addPaycheckScheduleUseCase(
                match {
                    it.frequency == PaycheckFrequency.MONTHLY &&
                        it.firstDay == 25 &&
                        it.secondDay == null
                },
                any()
            )
        }
    }

    @Test
    fun `generated occurrence suggests bills and exposes stop repeating`() = runTest {
        val occurrenceDate = dateMillis(2026, 11, 15)
        coEvery { getPaycheckUseCase(9L) } returns Paycheck(
            id = 9L,
            amount = 2_000.0,
            payDateMillis = occurrenceDate,
            label = "Salary",
            billIds = emptyList(),
            scheduleClientId = "schedule-1",
            occurrenceDateMillis = occurrenceDate
        )
        val viewModel = createViewModel(
            paychecksFlow = MutableStateFlow(
                listOf(
                    Paycheck(
                        id = 9L,
                        amount = 2_000.0,
                        payDateMillis = occurrenceDate,
                        billIds = emptyList(),
                        scheduleClientId = "schedule-1",
                        occurrenceDateMillis = occurrenceDate
                    )
                )
            )
        )

        viewModel.load(9L)
        advanceUntilIdle()

        val form = viewModel.uiState.value.form
        assertTrue(form.isRecurringOccurrence)
        assertEquals("schedule-1", form.scheduleClientId)
        assertFalse(form.canEditPayDate)
        assertEquals(
            expectedSuggestions(occurrenceDate),
            form.selectedBillIds
        )

        viewModel.effect.test {
            viewModel.onIntent(PaycheckFormIntent.StopRepeating)
            advanceUntilIdle()

            assertEquals(PaycheckFormEffect.RecurringStopped, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertTrue(viewModel.uiState.value.form.scheduleStopped)
        coVerify(exactly = 1) { stopPaycheckScheduleUseCase("schedule-1") }
    }

    @Test
    fun `editing an occurrence updates only that paycheck`() = runTest {
        val occurrenceDate = dateMillis(2026, 11, 15)
        coEvery { getPaycheckUseCase(9L) } returns Paycheck(
            id = 9L,
            amount = 2_000.0,
            payDateMillis = occurrenceDate,
            billIds = listOf(1L),
            scheduleClientId = "schedule-1",
            occurrenceDateMillis = occurrenceDate
        )
        val viewModel = createViewModel()
        viewModel.load(9L)
        advanceUntilIdle()
        viewModel.onIntent(PaycheckFormIntent.AmountChanged("2,500"))

        viewModel.onIntent(PaycheckFormIntent.Save)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            updatePaycheckUseCase(
                match {
                    it.id == 9L &&
                        it.amount == 2500.0 &&
                        it.payDateMillis == occurrenceDate
                }
            )
        }
        coVerify(exactly = 0) { addPaycheckScheduleUseCase(any(), any()) }
        coVerify(exactly = 0) { stopPaycheckScheduleUseCase(any()) }
    }

    @Test
    fun `a covered bill shows the cycle the paycheck claimed after it is paid`() = runTest {
        val payDate = dateMillis(2026, 11, 10)
        // Spotify was due Nov 10 and covered by this paycheck. Paying it
        // advanced the row to Dec 10 and cleared the paid flag.
        val spotify = Bill(
            id = 4,
            name = "Spotify",
            amount = 200.0,
            dueDateMillis = dateMillis(2026, 12, 10),
            frequency = BillFrequency.MONTHLY
        )
        coEvery { getPaycheckUseCase(9L) } returns Paycheck(
            id = 9L,
            amount = 2_000.0,
            payDateMillis = payDate,
            billIds = listOf(4L)
        )
        every { getBillPaymentHistoryUseCase() } returns flowOf(
            listOf(
                BillPayment(
                    billId = 4L,
                    amount = 200.0,
                    paidAtMillis = dateMillis(2026, 11, 12)
                )
            )
        )
        val viewModel = createViewModel(billsFlow = MutableStateFlow(listOf(spotify)))

        viewModel.load(9L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(payDate, state.coveredDueMillis(spotify))
        assertTrue(isOccurrencePaid(spotify, state.coveredDueMillis(spotify)))
    }

    @Test
    fun `editing the next recurring cutoff keeps the recurring bill selected`() = runTest {
        val spotify = Bill(
            id = 4,
            name = "Spotify",
            amount = 200.0,
            dueDateMillis = dateMillis(2026, 11, 15),
            frequency = BillFrequency.MONTHLY
        )
        val paychecks = MutableStateFlow(
            listOf(
                Paycheck(
                    id = 1,
                    amount = 2_000.0,
                    payDateMillis = dateMillis(2026, 11, 15),
                    scheduleClientId = "schedule-1"
                ),
                Paycheck(
                    id = 2,
                    amount = 2_000.0,
                    payDateMillis = dateMillis(2026, 12, 15),
                    scheduleClientId = "schedule-1"
                )
            )
        )
        coEvery { getPaycheckUseCase(2L) } returns paychecks.value[1]
        val viewModel = createViewModel(
            billsFlow = MutableStateFlow(listOf(spotify)),
            paychecksFlow = paychecks
        )

        viewModel.load(2L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(4L in state.form.selectedBillIds)
        assertFalse(4L in state.coveredBillIds)
        assertEquals(dateMillis(2026, 12, 15), state.coveredDueMillis(spotify))
    }

    @Test
    fun `a cutoff covers the bills due before the next pay date`() = runTest {
        // Maynilad is due Nov 20 and Spotify Nov 15. Both are due before the
        // Dec 30 cutoff, so the Dec 15 cutoff covers both.
        val spotify = Bill(
            id = 1,
            name = "Spotify",
            amount = 200.0,
            dueDateMillis = dateMillis(2026, 11, 15),
            frequency = BillFrequency.MONTHLY
        )
        val maynilad = Bill(
            id = 2,
            name = "Maynilad",
            amount = 500.0,
            dueDateMillis = dateMillis(2026, 11, 20),
            frequency = BillFrequency.MONTHLY
        )
        val paychecks = MutableStateFlow(
            listOf(
                Paycheck(
                    id = 1,
                    amount = 2_000.0,
                    payDateMillis = dateMillis(2026, 11, 15),
                    scheduleClientId = "schedule-1"
                ),
                Paycheck(
                    id = 2,
                    amount = 2_000.0,
                    payDateMillis = dateMillis(2026, 11, 30),
                    scheduleClientId = "schedule-2"
                ),
                Paycheck(
                    id = 3,
                    amount = 2_000.0,
                    payDateMillis = dateMillis(2026, 12, 15),
                    scheduleClientId = "schedule-1"
                ),
                Paycheck(
                    id = 4,
                    amount = 2_000.0,
                    payDateMillis = dateMillis(2026, 12, 30),
                    scheduleClientId = "schedule-2"
                )
            )
        )
        coEvery { getPaycheckUseCase(3L) } returns paychecks.value[2]
        val viewModel = createViewModel(
            billsFlow = MutableStateFlow(listOf(spotify, maynilad)),
            paychecksFlow = paychecks
        )

        viewModel.load(3L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(setOf(1L, 2L), state.form.selectedBillIds)
        assertEquals(dateMillis(2026, 12, 15), state.coveredDueMillis(spotify))
        assertEquals(dateMillis(2026, 12, 20), state.coveredDueMillis(maynilad))
    }

    @Test
    fun `bills to cover are listed by the date each row shows`() {
        val spotify = Bill(
            id = 1,
            name = "Spotify",
            amount = 250.0,
            dueDateMillis = dateMillis(2026, 11, 15),
            frequency = BillFrequency.MONTHLY
        )
        val maynilad = Bill(
            id = 2,
            name = "Maynilad",
            amount = 500.0,
            dueDateMillis = dateMillis(2026, 11, 30),
            frequency = BillFrequency.MONTHLY
        )
        val netflix = Bill(
            id = 3,
            name = "Netflix",
            amount = 250.0,
            dueDateMillis = dateMillis(2026, 11, 20)
        )
        val state = PaycheckFormUiState(
            form = PaycheckFormState(
                payDateMillis = dateMillis(2026, 11, 15),
                selectedBillIds = setOf(1L, 2L)
            ),
            bills = listOf(maynilad, netflix, spotify),
            dueByBillId = mapOf(
                1L to dateMillis(2026, 11, 15),
                2L to dateMillis(2026, 11, 30)
            ),
            zoneId = ZoneOffset.UTC
        )

        // Selected rows use their covered cycle; unselected rows their own date.
        assertEquals(
            listOf(1L, 3L, 2L),
            state.assignableBills().map { it.id }
        )
    }

    @Test
    fun `unchecking a planned bill keeps it under its month`() = runTest {
        // Rent is due on the 30th; the Dec 30 cutoff covers its December cycle.
        val rent = Bill(
            id = 1,
            name = "Rent",
            amount = 3_500.0,
            dueDateMillis = dateMillis(2026, 11, 30),
            frequency = BillFrequency.MONTHLY
        )
        val paychecks = MutableStateFlow(
            listOf(
                Paycheck(id = 7, amount = 22_000.0, payDateMillis = dateMillis(2026, 11, 30)),
                Paycheck(id = 8, amount = 22_000.0, payDateMillis = dateMillis(2026, 12, 15)),
                Paycheck(id = 9, amount = 22_000.0, payDateMillis = dateMillis(2026, 12, 30))
            )
        )
        coEvery { getPaycheckUseCase(9L) } returns paychecks.value[2]
        val viewModel = createViewModel(
            billsFlow = MutableStateFlow(listOf(rent)),
            paychecksFlow = paychecks
        )

        viewModel.load(9L)
        advanceUntilIdle()
        assertEquals(setOf(1L), viewModel.uiState.value.form.selectedBillIds)
        assertEquals(
            dateMillis(2026, 12, 30),
            viewModel.uiState.value.displayedDueMillis(rent)
        )

        viewModel.onIntent(PaycheckFormIntent.BillToggled(1L))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.form.selectedBillIds.isEmpty())
        // Still listed under December with the cycle this cutoff covers, instead
        // of dropping to the bill row's November date and moving away.
        assertEquals(listOf(1L), state.assignableBills().map { it.id })
        assertEquals(dateMillis(2026, 12, 30), state.displayedDueMillis(rent))
        assertTrue(1L in state.suggestedBillIds)
    }

    @Test
    fun `a saved cutoff still dates an unchecked bill to its month`() = runTest {
        val rent = Bill(
            id = 1,
            name = "Rent",
            amount = 3_500.0,
            dueDateMillis = dateMillis(2026, 11, 30),
            frequency = BillFrequency.MONTHLY
        )
        val internet = Bill(
            id = 2,
            name = "Internet",
            amount = 900.0,
            dueDateMillis = dateMillis(2027, 1, 5),
            frequency = BillFrequency.MONTHLY
        )
        val paychecks = MutableStateFlow(
            listOf(
                Paycheck(id = 7, amount = 22_000.0, payDateMillis = dateMillis(2026, 11, 30)),
                Paycheck(id = 8, amount = 22_000.0, payDateMillis = dateMillis(2026, 12, 15)),
                Paycheck(
                    id = 9,
                    amount = 22_000.0,
                    payDateMillis = dateMillis(2026, 12, 30),
                    billIds = listOf(2L)
                )
            )
        )
        coEvery { getPaycheckUseCase(9L) } returns paychecks.value[2]
        val viewModel = createViewModel(
            billsFlow = MutableStateFlow(listOf(rent, internet)),
            paychecksFlow = paychecks
        )

        viewModel.load(9L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // Only the saved bill is planned, but Rent is still listed under the
        // December cycle this cutoff would cover.
        assertEquals(setOf(2L), state.form.selectedBillIds)
        assertEquals(dateMillis(2026, 12, 30), state.displayedDueMillis(rent))
        assertEquals(listOf(1L, 2L), state.assignableBills().map { it.id })
        assertFalse(1L in state.suggestedBillIds)
    }

    private fun dateMillis(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

    private fun String.clean(): Double = replace(",", "").trim().toDouble()
}
