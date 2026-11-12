package com.smartexpense.bills

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartexpense.bills.screen.BillHistoryScreen
import com.smartexpense.bills.viewmodel.BillHistoryViewModel
import com.smartexpense.core.common.toDateString
import com.smartexpense.core.common.toPesoAmount
import com.smartexpense.domain.model.BillPayment
import com.smartexpense.domain.model.PaymentMethod
import com.smartexpense.domain.usecase.GetBillPaymentHistoryUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class BillHistoryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

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
        name: String,
        amount: Double,
        paidAt: String,
        method: PaymentMethod
    ) = BillPayment(
        id = id,
        billId = 1L,
        billName = name,
        amount = amount,
        paidAtMillis = Instant.parse(paidAt).toEpochMilli(),
        paymentMethod = method
    )

    private fun setContent(onNavigateBack: () -> Unit = {}) {
        composeRule.setContent {
            BillHistoryScreen(
                viewModel = viewModel,
                onNavigateBack = onNavigateBack
            )
        }
    }

    @Test
    fun showsTitleAndSubtitle() {
        setContent()

        composeRule.onNodeWithText("Payment history").assertIsDisplayed()
        composeRule.onNodeWithText("Bills you've already paid").assertIsDisplayed()
    }

    @Test
    fun showsEmptyStateWithoutPayments() {
        setContent()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.isLoading.not()
        }

        composeRule.onNodeWithText("No payments yet").assertIsDisplayed()
        composeRule.onNodeWithText("Bills you mark as paid will show up here.")
            .assertIsDisplayed()
    }

    @Test
    fun backActionIsForwarded() {
        var backClicked = false
        setContent(onNavigateBack = { backClicked = true })

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForIdle()

        assertTrue(backClicked)
    }

    @Test
    fun showsThisAndLastMonthSummaries() {
        paymentsFlow.value = listOf(
            payment(1, "Meralco", 1000.0, "2026-11-05T08:00:00Z", PaymentMethod.E_WALLET),
            payment(2, "Maynilad", 250.0, "2026-11-06T08:00:00Z", PaymentMethod.CASH),
            payment(3, "Meralco", 500.0, "2026-10-20T08:00:00Z", PaymentMethod.CASH),
            payment(4, "Maynilad", 400.0, "2026-10-21T08:00:00Z", PaymentMethod.E_WALLET)
        )
        setContent()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.months.isNotEmpty()
        }

        composeRule.onAllNodesWithText("This month").assertCountEquals(1)
        composeRule.onAllNodesWithText("Last month").assertCountEquals(1)
        // Totals only exist on the summary cards, since each row shows its own amount.
        composeRule.onAllNodesWithText(1250.0.toPesoAmount()).assertCountEquals(1)
        composeRule.onAllNodesWithText(900.0.toPesoAmount()).assertCountEquals(1)
    }

    @Test
    fun groupsPaymentsUnderDatedMonthHeaders() {
        paymentsFlow.value = listOf(
            payment(1, "Meralco", 1250.0, "2026-11-05T08:00:00Z", PaymentMethod.E_WALLET),
            payment(2, "Maynilad", 900.0, "2026-10-20T08:00:00Z", PaymentMethod.CASH)
        )
        setContent()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.months.isNotEmpty()
        }

        composeRule.onNodeWithText("This month • November 2026").assertIsDisplayed()
        composeRule.onNodeWithText("Last month • October 2026").assertIsDisplayed()
    }

    @Test
    fun paymentRowShowsBillDateMethodAndAmount() {
        val paidAt = Instant.parse("2026-11-05T08:00:00Z").toEpochMilli()
        paymentsFlow.value = listOf(
            payment(1, "Meralco", 1250.0, "2026-11-05T08:00:00Z", PaymentMethod.E_WALLET)
        )
        setContent()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.months.isNotEmpty()
        }

        composeRule.onNodeWithText("Meralco").assertIsDisplayed()
        composeRule.onNodeWithText("Paid on ${paidAt.toDateString()}").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Mode of Payment: ${PaymentMethod.E_WALLET.displayName}"
        ).assertIsDisplayed()
        // Shown once on the row and once on the "This month" summary card.
        composeRule.onAllNodesWithText(1250.0.toPesoAmount()).assertCountEquals(2)
    }

    @Test
    fun fallbackNameIsShownWhenPaymentHasNoBillName() {
        paymentsFlow.value = listOf(
            payment(1, "", 100.0, "2026-11-05T08:00:00Z", PaymentMethod.CASH)
        )
        setContent()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.months.isNotEmpty()
        }

        composeRule.onNodeWithText("Bill payment").assertIsDisplayed()
    }

    @Test
    fun monthFilterDefaultsToAllMonths() {
        paymentsFlow.value = listOf(
            payment(1, "Meralco", 1250.0, "2026-11-05T08:00:00Z", PaymentMethod.E_WALLET),
            payment(2, "Maynilad", 900.0, "2026-10-20T08:00:00Z", PaymentMethod.CASH)
        )
        setContent()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.months.isNotEmpty()
        }

        composeRule.onNodeWithText("Month").assertIsDisplayed()
        composeRule.onNodeWithText("All months").assertIsDisplayed()
    }

    @Test
    fun selectingAMonthShowsOnlyThatMonthsPayments() {
        paymentsFlow.value = listOf(
            payment(1, "Meralco", 1250.0, "2026-11-05T08:00:00Z", PaymentMethod.E_WALLET),
            payment(2, "Maynilad", 900.0, "2026-10-20T08:00:00Z", PaymentMethod.CASH)
        )
        setContent()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.months.isNotEmpty()
        }

        composeRule.onNodeWithText("All months").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("October 2026").performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText("Maynilad").assertCountEquals(1)
        composeRule.onAllNodesWithText("Meralco").assertCountEquals(0)
        composeRule.onAllNodesWithText("Last month • October 2026")
            .assertCountEquals(1)
        composeRule.onAllNodesWithText("This month • November 2026")
            .assertCountEquals(0)
    }

    @Test
    fun choosingAllMonthsRestoresTheFullHistory() {
        paymentsFlow.value = listOf(
            payment(1, "Meralco", 1250.0, "2026-11-05T08:00:00Z", PaymentMethod.E_WALLET),
            payment(2, "Maynilad", 900.0, "2026-10-20T08:00:00Z", PaymentMethod.CASH)
        )
        setContent()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.months.isNotEmpty()
        }

        composeRule.onNodeWithText("All months").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("October 2026").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("October 2026").performClick()
        composeRule.waitForIdle()
        // The menu reopens; choose "All months" from the menu.
        composeRule.onNodeWithText("All months").performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText("Maynilad").assertCountEquals(1)
        composeRule.onAllNodesWithText("Meralco").assertCountEquals(1)
    }
}
