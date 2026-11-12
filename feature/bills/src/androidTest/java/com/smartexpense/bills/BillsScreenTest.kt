package com.smartexpense.bills

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartexpense.bills.screen.BillsScreen
import com.smartexpense.bills.viewmodel.BillsViewModel
import com.smartexpense.core.common.toPesoAmount
import com.smartexpense.domain.model.Bill
import com.smartexpense.domain.model.BillAmountType
import com.smartexpense.domain.model.BillFrequency
import com.smartexpense.domain.model.Category
import com.smartexpense.domain.model.PaymentMethod
import com.smartexpense.domain.usecase.CategoryUseCase
import com.smartexpense.domain.usecase.GetBillsUseCase
import com.smartexpense.domain.usecase.PayBillUseCase
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class BillsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var getBillsUseCase: GetBillsUseCase
    private lateinit var payBillUseCase: PayBillUseCase
    private lateinit var categoryUseCase: CategoryUseCase

    private lateinit var billsFlow: MutableStateFlow<List<Bill>>
    private lateinit var categoriesFlow: MutableStateFlow<List<Category>>

    private lateinit var viewModel: BillsViewModel

    @Before
    fun setup() {
        getBillsUseCase = mockk()
        payBillUseCase = mockk()
        categoryUseCase = mockk()

        billsFlow = MutableStateFlow(emptyList())
        categoriesFlow = MutableStateFlow(listOf(Category(id = 1L, name = "Utilities")))

        every { getBillsUseCase() } returns billsFlow
        every { categoryUseCase.getCategories() } returns categoriesFlow
        coEvery {
            payBillUseCase(any(), any(), any(), any(), any(), any())
        } just Runs

        viewModel = BillsViewModel(
            getBillsUseCase = getBillsUseCase,
            payBillUseCase = payBillUseCase,
            categoryUseCase = categoryUseCase
        )
    }

    private fun todayMillis(): Long =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun bill(
        id: Long = 1L,
        name: String = "Meralco",
        amount: Double = 4750.0,
        amountType: BillAmountType = BillAmountType.VARIABLE,
        amountEstimated: Boolean = true,
        frequency: BillFrequency = BillFrequency.MONTHLY,
        categoryId: Long? = 1L,
        payee: String = "Meralco"
    ) = Bill(
        id = id,
        name = name,
        amount = amount,
        dueDateMillis = todayMillis(),
        frequency = frequency,
        amountType = amountType,
        amountEstimated = amountEstimated,
        categoryId = categoryId,
        payee = payee
    )

    private fun setContent(
        onNavigateBack: () -> Unit = {},
        onAddBill: () -> Unit = {},
        onHistory: () -> Unit = {},
        onEditBill: (Long) -> Unit = {}
    ) {
        composeRule.setContent {
            BillsScreen(
                viewModel = viewModel,
                onNavigateBack = onNavigateBack,
                onAddBill = onAddBill,
                onHistory = onHistory,
                onEditBill = onEditBill
            )
        }
    }

    private fun openPaySheet(billName: String = "Meralco") {
        composeRule.onNodeWithText(billName).assertIsDisplayed()
        composeRule.onNodeWithText("Mark paid").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Mark as paid")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    // --------------------------------------------------
    // Screen basics
    // --------------------------------------------------

    @Test
    fun screenShowsTitleAndSubtitle() {
        setContent()

        composeRule.onNodeWithText("Bills").assertIsDisplayed()
        composeRule.onNodeWithText("Track and pay your bills").assertIsDisplayed()
    }

    @Test
    fun emptyStateShownWithoutBills() {
        setContent()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("No bills yet").assertIsDisplayed()
        composeRule.onNodeWithText("Tap Add Bill to add your first bill.").assertIsDisplayed()
    }

    @Test
    fun addBillActionIsForwarded() {
        var addClicked = false
        setContent(onAddBill = { addClicked = true })

        composeRule.onNodeWithText("Add Bill").performClick()
        composeRule.waitForIdle()

        assertTrue(addClicked)
    }

    @Test
    fun historyActionIsForwarded() {
        var historyClicked = false
        setContent(onHistory = { historyClicked = true })

        composeRule.onNodeWithText("History").performClick()
        composeRule.waitForIdle()

        assertTrue(historyClicked)
    }

    // --------------------------------------------------
    // Bill cards
    // --------------------------------------------------

    @Test
    fun billCardShowsDetails() {
        billsFlow.value = listOf(bill())
        setContent()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Meralco").assertIsDisplayed()
        composeRule.onNodeWithText("Due today").assertIsDisplayed()
        composeRule.onNodeWithText("Monthly • Utilities").assertIsDisplayed()
        composeRule.onNodeWithText("Variable").assertIsDisplayed()
        composeRule.onNodeWithText("Est. ${4750.0.toPesoAmount()}").assertIsDisplayed()
        composeRule.onNodeWithText("Mark paid").assertIsDisplayed()
    }

    @Test
    fun fixedBillShowsFixedAmountWithoutEstimate() {
        billsFlow.value = listOf(
            bill(
                name = "Maynilad",
                amount = 900.0,
                amountType = BillAmountType.STATIC,
                amountEstimated = false
            )
        )
        setContent()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Fixed").assertIsDisplayed()
        composeRule.onNodeWithText(900.0.toPesoAmount()).assertIsDisplayed()
        composeRule.onAllNodesWithText("Est. ${900.0.toPesoAmount()}").assertCountEquals(0)
    }

    @Test
    fun tappingCardForwardsBillId() {
        billsFlow.value = listOf(bill(id = 42L))
        var editedId: Long? = null
        setContent(onEditBill = { editedId = it })
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Meralco").performClick()
        composeRule.waitForIdle()

        assertEquals(42L, editedId)
    }

    @Test
    fun paidBillShowsPaidStatusWithoutMarkPaidAction() {
        billsFlow.value = listOf(
            bill().copy(paidAtMillis = System.currentTimeMillis())
        )
        setContent()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText("Paid").fetchSemanticsNodes().isNotEmpty()
        composeRule.onAllNodesWithText("Mark paid").assertCountEquals(0)
    }

    // --------------------------------------------------
    // Mark-as-paid sheet
    // --------------------------------------------------

    @Test
    fun markPaidOpensSheetWithAllFields() {
        billsFlow.value = listOf(bill(amountType = BillAmountType.STATIC, amountEstimated = false))
        setContent()
        composeRule.waitForIdle()

        openPaySheet()

        composeRule.onNodeWithText("Amount paid").assertIsDisplayed()
        composeRule.onNodeWithText("Paid on").assertIsDisplayed()
        composeRule.onNodeWithText("Payment method").assertIsDisplayed()
        composeRule.onNodeWithText("Create expense").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
        composeRule.onNodeWithText("₱").assertIsDisplayed()
    }

    @Test
    fun variableBillSheetAsksForActualAmount() {
        billsFlow.value = listOf(bill())
        setContent()
        composeRule.waitForIdle()

        openPaySheet()

        composeRule.onNodeWithText("Actual amount paid").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Estimated ${4750.0.toPesoAmount()} based on recent payments. " +
                "Enter the actual amount from your bill."
        ).assertIsDisplayed()
    }

    @Test
    fun cancelClosesSheet() {
        billsFlow.value = listOf(bill())
        setContent()
        composeRule.waitForIdle()

        openPaySheet()
        composeRule.onNodeWithText("Cancel").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Mark as paid")
                .fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun paymentMethodDropdownListsOptionsAndSelects() {
        billsFlow.value = listOf(bill())
        setContent()
        composeRule.waitForIdle()

        openPaySheet()
        composeRule.onNodeWithText("Payment method").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Credit Card").assertIsDisplayed()
        composeRule.onNodeWithText("E-Wallet").assertIsDisplayed()

        composeRule.onNodeWithText("E-Wallet").performClick()
        composeRule.waitForIdle()

        // Menu is closed again and the field shows the selected method.
        composeRule.onAllNodesWithText("Credit Card").assertCountEquals(0)
        composeRule.onAllNodesWithText("E-Wallet").assertCountEquals(1)
    }

    @Test
    fun confirmingPaymentCallsPayUseCase() {
        billsFlow.value = listOf(bill(amountType = BillAmountType.STATIC, amountEstimated = false))
        setContent()
        composeRule.waitForIdle()

        openPaySheet()
        composeRule.onNodeWithTag("bill_pay_confirm").performScrollTo().performClick()

        coVerify(timeout = 5_000) {
            payBillUseCase(
                match { it.id == 1L },
                4750.0,
                any(),
                PaymentMethod.CASH,
                true,
                1L
            )
        }
    }
}
