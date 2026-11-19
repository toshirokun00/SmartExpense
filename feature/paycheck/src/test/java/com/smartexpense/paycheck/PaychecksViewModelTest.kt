package com.smartexpense.paycheck

import com.smartexpense.domain.model.Bill
import com.smartexpense.domain.model.BillFrequency
import com.smartexpense.domain.model.BillPayment
import com.smartexpense.domain.model.Paycheck
import com.smartexpense.domain.usecase.EnsurePaycheckOccurrencesUseCase
import com.smartexpense.domain.usecase.GetBillPaymentHistoryUseCase
import com.smartexpense.domain.usecase.GetBillsUseCase
import com.smartexpense.domain.usecase.GetPaychecksUseCase
import com.smartexpense.paycheck.intent.PaychecksIntent
import com.smartexpense.paycheck.state.PlannedBill
import com.smartexpense.paycheck.viewmodel.PaychecksViewModel
import com.smartexpense.test.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class PaychecksViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(
        Instant.parse("2026-11-13T12:00:00Z"),
        ZoneOffset.UTC
    )

    private fun millis(year: Int, month: Int, day: Int): Long =
        java.time.LocalDate.of(year, month, day)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

    private fun paychecksFlow() = MutableStateFlow(
        listOf(
            Paycheck(id = 1, amount = 1000.0, payDateMillis = millis(2026, 10, 31)),
            Paycheck(id = 2, amount = 1000.0, payDateMillis = millis(2026, 11, 15)),
            Paycheck(id = 3, amount = 1000.0, payDateMillis = millis(2026, 12, 31)),
            Paycheck(id = 4, amount = 1000.0, payDateMillis = millis(2027, 1, 15))
        )
    )

    private fun createViewModel(
        paychecks: MutableStateFlow<List<Paycheck>> = paychecksFlow(),
        bills: MutableStateFlow<List<Bill>> = MutableStateFlow(emptyList()),
        payments: MutableStateFlow<List<BillPayment>> = MutableStateFlow(emptyList()),
        onEnsure: (LocalDate, LocalDate) -> Unit = { _, _ -> }
    ): PaychecksViewModel {
        val getPaychecksUseCase = mockk<GetPaychecksUseCase>()
        val getBillsUseCase = mockk<GetBillsUseCase>()
        val getBillPaymentHistoryUseCase = mockk<GetBillPaymentHistoryUseCase>()
        val ensurePaycheckOccurrencesUseCase = mockk<EnsurePaycheckOccurrencesUseCase>()
        every { getPaychecksUseCase() } returns paychecks
        every { getBillsUseCase() } returns bills
        every { getBillPaymentHistoryUseCase() } returns payments
        coEvery {
            ensurePaycheckOccurrencesUseCase(any(), any())
        } coAnswers {
            onEnsure(firstArg(), secondArg())
        }
        return PaychecksViewModel(
            getPaychecksUseCase = getPaychecksUseCase,
            getBillsUseCase = getBillsUseCase,
            getBillPaymentHistoryUseCase = getBillPaymentHistoryUseCase,
            ensurePaycheckOccurrencesUseCase = ensurePaycheckOccurrencesUseCase,
            clock = clock
        )
    }

    @Test
    fun `collects paychecks and bills`() = runTest {
        val billsFlow = MutableStateFlow(
            listOf(
                Bill(
                    id = 2,
                    name = "Rent",
                    amount = 500.0,
                    dueDateMillis = 0L
                )
            )
        )
        val viewModel = createViewModel(bills = billsFlow)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.paychecks.size)
        assertEquals(1, viewModel.uiState.value.bills.size)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `shows only the current month by default`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(YearMonth.of(2026, 11), state.selectedMonth)
        assertEquals(YearMonth.of(2026, 11), state.currentMonth)
        assertEquals(listOf(2L), state.paychecks.map { it.id })
    }

    @Test
    fun `moves to the previous and next month`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(PaychecksIntent.PreviousMonth)
        assertEquals(YearMonth.of(2026, 10), viewModel.uiState.value.selectedMonth)
        assertEquals(listOf(1L), viewModel.uiState.value.paychecks.map { it.id })

        viewModel.onIntent(PaychecksIntent.NextMonth)
        assertEquals(YearMonth.of(2026, 11), viewModel.uiState.value.selectedMonth)
        assertEquals(listOf(2L), viewModel.uiState.value.paychecks.map { it.id })

        viewModel.onIntent(PaychecksIntent.NextMonth)
        assertEquals(YearMonth.of(2026, 12), viewModel.uiState.value.selectedMonth)
        assertEquals(listOf(3L), viewModel.uiState.value.paychecks.map { it.id })
    }

    @Test
    fun `jumps to a specific month and back to this month`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(PaychecksIntent.SelectMonth(2027, 1))

        assertEquals(YearMonth.of(2027, 1), viewModel.uiState.value.selectedMonth)
        assertEquals(listOf(4L), viewModel.uiState.value.paychecks.map { it.id })

        viewModel.onIntent(PaychecksIntent.CurrentMonth)

        assertEquals(YearMonth.of(2026, 11), viewModel.uiState.value.selectedMonth)
        assertEquals(listOf(2L), viewModel.uiState.value.paychecks.map { it.id })
    }

    @Test
    fun `materializes the paychecks of every browsed month`() = runTest {
        val ensured = mutableListOf<Pair<LocalDate, LocalDate>>()
        val paychecks = paychecksFlow()
        val viewModel = createViewModel(
            paychecks = paychecks,
            onEnsure = { from, to ->
                ensured.add(from to to)
                if (YearMonth.from(to) == YearMonth.of(2027, 2)) {
                    paychecks.value = paychecks.value + Paycheck(
                        id = 6,
                        amount = 1000.0,
                        payDateMillis = millis(2027, 2, 15)
                    )
                }
            }
        )
        advanceUntilIdle()

        // The month on screen at startup is filled in as well.
        assertEquals(
            LocalDate.of(2026, 11, 1) to LocalDate.of(2026, 11, 30),
            ensured.first()
        )

        viewModel.onIntent(PaychecksIntent.SelectMonth(2027, 2))
        advanceUntilIdle()

        assertEquals(
            LocalDate.of(2027, 2, 1) to LocalDate.of(2027, 2, 28),
            ensured.last()
        )
        assertEquals(listOf(6L), viewModel.uiState.value.paychecks.map { it.id })
    }

    @Test
    fun `keeps the selected month when paychecks change`() = runTest {
        val paychecks = paychecksFlow()
        val viewModel = createViewModel(paychecks = paychecks)
        advanceUntilIdle()

        viewModel.onIntent(PaychecksIntent.SelectMonth(2026, 12))
        paychecks.value = paychecks.value + Paycheck(
            id = 5,
            amount = 750.0,
            payDateMillis = millis(2026, 12, 20)
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(YearMonth.of(2026, 12), state.selectedMonth)
        assertEquals(listOf(3L, 5L), state.paychecks.map { it.id })
    }

    @Test
    fun `an empty month shows no paychecks`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(PaychecksIntent.SelectMonth(2025, 6))

        assertEquals(emptyList<Paycheck>(), viewModel.uiState.value.paychecks)
    }

    @Test
    fun `upcoming paychecks project their uncovered bills`() = runTest {
        val bills = MutableStateFlow(
            listOf(
                bill(id = 1, day = 5, month = 11),
                bill(id = 2, day = 20, month = 12)
            )
        )
        val viewModel = createViewModel(bills = bills)
        advanceUntilIdle()

        viewModel.onIntent(PaychecksIntent.SelectMonth(2026, 12))
        val state = viewModel.uiState.value

        // Bill 1 belongs to the Oct 31 cutoff, bill 2 to the Nov 15 one, which
        // is the paycheck that comes before its due date.
        assertEquals(listOf(1L), state.plannedBills[1L]?.map { it.billId })
        assertEquals(listOf(2L), state.plannedBills[2L]?.map { it.billId })
        assertEquals(emptyList<Long>(), state.plannedBills[3L]?.map { it.billId })
    }

    @Test
    fun `paychecks cover the bills due before the next pay date`() = runTest {
        val bills = MutableStateFlow(
            listOf(
                bill(id = 1, day = 5, month = 11),
                bill(id = 2, day = 20, month = 11),
                bill(id = 3, day = 2, month = 12)
            )
        )
        val paychecks = MutableStateFlow(
            listOf(
                Paycheck(id = 1, amount = 1000.0, payDateMillis = millis(2026, 11, 15)),
                Paycheck(id = 2, amount = 1000.0, payDateMillis = millis(2026, 11, 30))
            )
        )
        val viewModel = createViewModel(paychecks = paychecks, bills = bills)
        advanceUntilIdle()

        val planned = viewModel.uiState.value.plannedBills

        // Nov 5 and Nov 20 are before the Nov 30 paycheck; Dec 2 is before the
        // cutoff after it.
        assertEquals(listOf(1L, 2L), planned[1L]?.map { it.billId })
        assertEquals(listOf(3L), planned[2L]?.map { it.billId })
    }

    @Test
    fun `an uncovered bill is only projected on the first paycheck that can cover it`() = runTest {
        val bills = MutableStateFlow(listOf(bill(id = 1, day = 10, month = 12)))
        val paychecks = MutableStateFlow(
            listOf(
                Paycheck(id = 3, amount = 1000.0, payDateMillis = millis(2026, 12, 15)),
                Paycheck(id = 4, amount = 1000.0, payDateMillis = millis(2026, 12, 31))
            )
        )
        val viewModel = createViewModel(paychecks = paychecks, bills = bills)
        advanceUntilIdle()

        val planned = viewModel.uiState.value.plannedBills

        assertEquals(listOf(1L), planned[3L]?.map { it.billId })
        assertEquals(emptyList<Long>(), planned[4L]?.map { it.billId })
    }

    @Test
    fun `saved assignments win over projections`() = runTest {
        val bills = MutableStateFlow(
            listOf(
                bill(id = 1, day = 5, month = 12),
                bill(id = 2, day = 10, month = 12)
            )
        )
        val paychecks = MutableStateFlow(
            listOf(
                Paycheck(
                    id = 3,
                    amount = 1000.0,
                    payDateMillis = millis(2026, 12, 31),
                    billIds = listOf(2L)
                )
            )
        )
        val viewModel = createViewModel(paychecks = paychecks, bills = bills)
        advanceUntilIdle()

        assertEquals(listOf(2L), viewModel.uiState.value.plannedBills[3L]?.map { it.billId })
    }

    @Test
    fun `projections refresh when bills change`() = runTest {
        val bills = MutableStateFlow(emptyList<Bill>())
        val viewModel = createViewModel(bills = bills)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.plannedBills[2L].isNullOrEmpty())

        bills.value = listOf(bill(id = 1, day = 20, month = 12))
        advanceUntilIdle()

        assertEquals(listOf(1L), viewModel.uiState.value.plannedBills[2L]?.map { it.billId })
    }

    @Test
    fun `a paid recurring bill stays pinned to the cycle the paycheck covered`() = runTest {
        // Spotify was due Nov 15 and covered by the Nov 15 paycheck. Paying
        // it advances the row to Dec 15 and clears the paid flag.
        val spotify = Bill(
            id = 1,
            name = "Spotify",
            amount = 200.0,
            dueDateMillis = millis(2026, 12, 15),
            frequency = BillFrequency.MONTHLY
        )
        val paychecks = MutableStateFlow(
            listOf(
                Paycheck(
                    id = 2,
                    amount = 1000.0,
                    payDateMillis = millis(2026, 11, 15),
                    billIds = listOf(1L)
                )
            )
        )
        val payments = MutableStateFlow(
            listOf(
                BillPayment(
                    billId = 1L,
                    amount = 200.0,
                    paidAtMillis = millis(2026, 11, 20)
                )
            )
        )
        val viewModel = createViewModel(
            paychecks = paychecks,
            bills = MutableStateFlow(listOf(spotify)),
            payments = payments
        )
        advanceUntilIdle()

        val planned = viewModel.uiState.value.plannedBills[2L].orEmpty()

        assertEquals(
            listOf(
                PlannedBill(
                    billId = 1L,
                    dueDateMillis = millis(2026, 11, 15),
                    isPaid = true
                )
            ),
            planned
        )
    }

    @Test
    fun `an unpaid covered bill shows its cycle as unpaid`() = runTest {
        val spotify = Bill(
            id = 1,
            name = "Spotify",
            amount = 200.0,
            dueDateMillis = millis(2026, 11, 15),
            frequency = BillFrequency.MONTHLY
        )
        val paychecks = MutableStateFlow(
            listOf(
                Paycheck(
                    id = 2,
                    amount = 1000.0,
                    payDateMillis = millis(2026, 11, 15),
                    billIds = listOf(1L)
                )
            )
        )
        val viewModel = createViewModel(
            paychecks = paychecks,
            bills = MutableStateFlow(listOf(spotify))
        )
        advanceUntilIdle()

        assertEquals(
            listOf(
                PlannedBill(
                    billId = 1L,
                    dueDateMillis = millis(2026, 11, 15),
                    isPaid = false
                )
            ),
            viewModel.uiState.value.plannedBills[2L]
        )
    }

    @Test
    fun `a payment made before the pay date still pins the settled cycle`() = runTest {
        // Meralco's November cycle was paid on Nov 11, which advanced the row to
        // Dec 9. The Nov 29 cutoff still shows the cycle that was settled.
        val meralco = Bill(
            id = 1,
            name = "Meralco",
            amount = 4750.0,
            dueDateMillis = millis(2026, 12, 9),
            frequency = BillFrequency.MONTHLY
        )
        val paychecks = MutableStateFlow(
            listOf(
                Paycheck(
                    id = 2,
                    amount = 3000.0,
                    payDateMillis = millis(2026, 11, 29),
                    billIds = listOf(1L)
                )
            )
        )
        val payments = MutableStateFlow(
            listOf(
                BillPayment(
                    billId = 1L,
                    amount = 4750.0,
                    paidAtMillis = millis(2026, 11, 11)
                )
            )
        )
        val viewModel = createViewModel(
            paychecks = paychecks,
            bills = MutableStateFlow(listOf(meralco)),
            payments = payments
        )
        advanceUntilIdle()

        assertEquals(
            listOf(
                PlannedBill(
                    billId = 1L,
                    dueDateMillis = millis(2026, 11, 9),
                    isPaid = true
                )
            ),
            viewModel.uiState.value.plannedBills[2L]
        )
    }

    @Test
    fun `paying every covered bill keeps the rows on the paycheck`() = runTest {
        // The reported bug: marking all bills paid emptied the paycheck card.
        val electricity = Bill(
            id = 1,
            name = "Meralco",
            amount = 1000.0,
            dueDateMillis = millis(2026, 12, 1),
            frequency = BillFrequency.MONTHLY
        )
        val internet = Bill(
            id = 2,
            name = "Netflix",
            amount = 250.0,
            dueDateMillis = millis(2026, 12, 15),
            frequency = BillFrequency.MONTHLY
        )
        val paychecks = MutableStateFlow(
            listOf(
                Paycheck(
                    id = 2,
                    amount = 5000.0,
                    payDateMillis = millis(2026, 11, 15),
                    scheduleClientId = "schedule-1"
                )
            )
        )
        val payments = MutableStateFlow(
            listOf(
                BillPayment(billId = 1L, amount = 1000.0, paidAtMillis = millis(2026, 11, 1)),
                BillPayment(billId = 2L, amount = 250.0, paidAtMillis = millis(2026, 11, 15))
            )
        )
        val viewModel = createViewModel(
            paychecks = paychecks,
            bills = MutableStateFlow(listOf(electricity, internet)),
            payments = payments
        )
        advanceUntilIdle()

        val planned = viewModel.uiState.value.plannedBills[2L].orEmpty()

        assertEquals(listOf(1L, 2L), planned.map { it.billId })
        assertEquals(millis(2026, 11, 1), planned[0].dueDateMillis)
        assertEquals(millis(2026, 11, 15), planned[1].dueDateMillis)
        assertTrue(planned.all { it.isPaid })
    }

    @Test
    fun `covered bills are listed by due date`() = runTest {
        // Saved order is reversed; the card must read in payment order.
        val rent = Bill(
            id = 1,
            name = "Rent",
            amount = 1000.0,
            dueDateMillis = millis(2026, 11, 20),
            frequency = BillFrequency.MONTHLY
        )
        val power = Bill(
            id = 2,
            name = "Meralco",
            amount = 900.0,
            dueDateMillis = millis(2026, 11, 5),
            frequency = BillFrequency.MONTHLY
        )
        val paychecks = MutableStateFlow(
            listOf(
                Paycheck(
                    id = 1,
                    amount = 5000.0,
                    payDateMillis = millis(2026, 11, 15),
                    billIds = listOf(1L, 2L)
                )
            )
        )
        val viewModel = createViewModel(
            paychecks = paychecks,
            bills = MutableStateFlow(listOf(rent, power))
        )
        advanceUntilIdle()

        assertEquals(
            listOf(2L, 1L),
            viewModel.uiState.value.plannedBills[1L]?.map { it.billId }
        )
    }

    @Test
    fun `a recurring bill shows its own cycle on the next months of a schedule`() = runTest {
        val spotify = Bill(
            id = 1,
            name = "Spotify",
            amount = 200.0,
            dueDateMillis = millis(2026, 11, 15),
            frequency = BillFrequency.MONTHLY
        )
        val paychecks = MutableStateFlow(
            listOf(
                Paycheck(
                    id = 1,
                    amount = 1000.0,
                    payDateMillis = millis(2026, 11, 15),
                    scheduleClientId = "schedule-1"
                ),
                Paycheck(
                    id = 2,
                    amount = 1000.0,
                    payDateMillis = millis(2026, 12, 15),
                    scheduleClientId = "schedule-1"
                ),
                Paycheck(
                    id = 3,
                    amount = 1000.0,
                    payDateMillis = millis(2027, 2, 15),
                    scheduleClientId = "schedule-1"
                )
            )
        )
        val viewModel = createViewModel(
            paychecks = paychecks,
            bills = MutableStateFlow(listOf(spotify))
        )
        advanceUntilIdle()

        viewModel.onIntent(PaychecksIntent.SelectMonth(2026, 12))
        val planned = viewModel.uiState.value.plannedBills[2L].orEmpty()

        assertEquals(listOf(1L), planned.map { it.billId })
        assertEquals(millis(2026, 12, 15), planned.single().dueDateMillis)

        // Later months of the schedule show their own cycle too.
        viewModel.onIntent(PaychecksIntent.SelectMonth(2027, 2))
        assertEquals(
            millis(2027, 2, 15),
            viewModel.uiState.value.plannedBills[3L]?.single()?.dueDateMillis
        )
    }

    private fun bill(id: Long, day: Int, month: Int): Bill = Bill(
        id = id,
        name = "Bill $id",
        amount = 100.0,
        dueDateMillis = millis(2026, month, day)
    )
}
