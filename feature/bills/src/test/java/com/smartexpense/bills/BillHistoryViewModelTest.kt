package com.smartexpense.bills

import com.smartexpense.bills.state.BillHistoryUiState
import com.smartexpense.bills.viewmodel.BillHistoryViewModel
import com.smartexpense.domain.model.BillPayment
import com.smartexpense.domain.usecase.GetBillPaymentHistoryUseCase
import com.smartexpense.test.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset

class BillHistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock: Clock = Clock.fixed(
        Instant.parse("2026-11-12T10:00:00Z"),
        ZoneOffset.UTC
    )

    private lateinit var getBillPaymentHistoryUseCase: GetBillPaymentHistoryUseCase
    private lateinit var paymentsFlow: MutableStateFlow<List<BillPayment>>

    private lateinit var viewModel: BillHistoryViewModel

    @Before
    fun setup() {
        getBillPaymentHistoryUseCase = mockk()
        paymentsFlow = MutableStateFlow(emptyList())
        every { getBillPaymentHistoryUseCase() } returns paymentsFlow

        viewModel = BillHistoryViewModel(
            getBillPaymentHistoryUseCase = getBillPaymentHistoryUseCase,
            clock = clock
        )
    }

    private fun payment(
        id: Long,
        paidAt: String,
        amount: Double
    ) = BillPayment(
        id = id,
        billId = 1L,
        billName = "Meralco",
        amount = amount,
        paidAtMillis = Instant.parse(paidAt).toEpochMilli()
    )

    @Test
    fun emptyHistoryFinishesLoadingWithNoMonths() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.months.isEmpty())
        assertTrue(state.isEmpty)
    }

    @Test
    fun loadsGroupedMonthsFromTheRepository() = runTest {
        paymentsFlow.value = listOf(
            payment(1, "2026-11-01T08:00:00Z", 1000.0),
            payment(2, "2026-10-20T08:00:00Z", 900.0)
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isEmpty)
        assertEquals(2, state.months.size)
        assertEquals("This month • November 2026", state.months.first().label)
    }

    @Test
    fun thisMonthSummaryOnlyCountsTheCurrentMonth() = runTest {
        paymentsFlow.value = listOf(
            payment(1, "2026-11-01T08:00:00Z", 1000.0),
            payment(2, "2026-11-05T08:00:00Z", 250.0),
            payment(3, "2026-10-20T08:00:00Z", 900.0)
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.thisMonthCount)
        assertEquals(1250.0, state.thisMonthTotal, 0.0)
        assertEquals(1, state.lastMonthCount)
        assertEquals(900.0, state.lastMonthTotal, 0.0)
    }

    @Test
    fun lastMonthSummaryIsZeroWhenLastMonthHasNoPayments() = runTest {
        paymentsFlow.value = listOf(
            payment(1, "2026-11-01T08:00:00Z", 1000.0),
            payment(2, "2026-09-05T08:00:00Z", 500.0)
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.lastMonthCount)
        assertEquals(0.0, state.lastMonthTotal, 0.0)
        assertEquals(1000.0, state.thisMonthTotal, 0.0)
    }

    @Test
    fun newPaymentsAreForwardedToTheState() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.months.isEmpty())

        paymentsFlow.value = listOf(payment(1, "2026-11-11T08:00:00Z", 700.0))
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.months.size)
        assertEquals(700.0, viewModel.uiState.value.thisMonthTotal, 0.0)
    }

    @Test
    fun selectingAMonthFiltersTheVisibleMonths() = runTest {
        paymentsFlow.value = listOf(
            payment(1, "2026-11-01T08:00:00Z", 1000.0),
            payment(2, "2026-10-20T08:00:00Z", 900.0),
            payment(3, "2026-09-20T08:00:00Z", 800.0)
        )
        advanceUntilIdle()

        viewModel.onMonthSelected(YearMonth.of(2026, 9))

        val state = viewModel.uiState.value
        assertEquals(listOf(YearMonth.of(2026, 9)), state.visibleMonths.map { it.yearMonth })
        assertEquals("September 2026", state.selectedMonthLabel)
    }

    @Test
    fun clearingTheFilterShowsEveryMonthAgain() = runTest {
        paymentsFlow.value = listOf(
            payment(1, "2026-11-01T08:00:00Z", 1000.0),
            payment(2, "2026-10-20T08:00:00Z", 900.0)
        )
        advanceUntilIdle()
        viewModel.onMonthSelected(YearMonth.of(2026, 10))

        viewModel.onMonthSelected(null)

        val state = viewModel.uiState.value
        assertEquals(2, state.visibleMonths.size)
        assertEquals(BillHistoryUiState.ALL_MONTHS_LABEL, state.selectedMonthLabel)
    }

    @Test
    fun selectedMonthResetsWhenItsPaymentsDisappear() = runTest {
        paymentsFlow.value = listOf(
            payment(1, "2026-11-01T08:00:00Z", 1000.0),
            payment(2, "2026-10-20T08:00:00Z", 900.0)
        )
        advanceUntilIdle()
        viewModel.onMonthSelected(YearMonth.of(2026, 10))

        paymentsFlow.value = listOf(payment(1, "2026-11-01T08:00:00Z", 1000.0))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.selectedMonth)
        assertEquals(1, viewModel.uiState.value.visibleMonths.size)
    }
}
