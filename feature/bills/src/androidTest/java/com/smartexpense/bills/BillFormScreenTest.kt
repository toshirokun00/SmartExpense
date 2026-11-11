package com.smartexpense.bills

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartexpense.bills.screen.BillFormScreen
import com.smartexpense.bills.viewmodel.BillFormViewModel
import com.smartexpense.domain.model.Bill
import com.smartexpense.domain.model.BillAmountType
import com.smartexpense.domain.model.BillFrequency
import com.smartexpense.domain.model.Category
import com.smartexpense.domain.model.PaymentMethod
import com.smartexpense.domain.usecase.AddBillUseCase
import com.smartexpense.domain.usecase.CategoryUseCase
import com.smartexpense.domain.usecase.DeleteBillUseCase
import com.smartexpense.domain.usecase.GetBillUseCase
import com.smartexpense.domain.usecase.SkipBillUseCase
import com.smartexpense.domain.usecase.UpdateBillUseCase
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BillFormScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var getBillUseCase: GetBillUseCase
    private lateinit var addBillUseCase: AddBillUseCase
    private lateinit var updateBillUseCase: UpdateBillUseCase
    private lateinit var deleteBillUseCase: DeleteBillUseCase
    private lateinit var skipBillUseCase: SkipBillUseCase
    private lateinit var categoryUseCase: CategoryUseCase

    private lateinit var viewModel: BillFormViewModel

    @Before
    fun setup() {
        getBillUseCase = mockk()
        addBillUseCase = mockk()
        updateBillUseCase = mockk()
        deleteBillUseCase = mockk()
        skipBillUseCase = mockk()
        categoryUseCase = mockk()

        every { categoryUseCase.getCategories() } returns flowOf(
            listOf(
                Category(id = 1L, name = "Utilities"),
                Category(id = 2L, name = "Internet")
            )
        )
        coEvery { addBillUseCase(any()) } returns 1L
        coEvery { updateBillUseCase(any()) } just Runs
        coEvery { deleteBillUseCase(any()) } just Runs
        coEvery { skipBillUseCase(any()) } just Runs

        viewModel = BillFormViewModel(
            getBillUseCase = getBillUseCase,
            addBillUseCase = addBillUseCase,
            updateBillUseCase = updateBillUseCase,
            deleteBillUseCase = deleteBillUseCase,
            skipBillUseCase = skipBillUseCase,
            categoryUseCase = categoryUseCase
        )
    }

    private fun bill() = Bill(
        id = 1L,
        name = "Meralco",
        amount = 4750.0,
        dueDateMillis = System.currentTimeMillis(),
        frequency = BillFrequency.MONTHLY,
        amountType = BillAmountType.VARIABLE,
        amountEstimated = true,
        categoryId = 1L,
        payee = "Meralco",
        paymentMethod = PaymentMethod.CASH
    )

    private fun setContent(billId: Long? = null) {
        composeRule.setContent {
            BillFormScreen(
                viewModel = viewModel,
                billId = billId,
                onNavigateBack = {},
                onSaved = {}
            )
        }
    }

    private fun scrollTo(text: String) {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(text))
        composeRule.waitForIdle()
    }

    // --------------------------------------------------
    // Add form
    // --------------------------------------------------

    @Test
    fun addFormShowsTitleAndFields() {
        setContent()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Add Bill").assertIsDisplayed()
        composeRule.onNodeWithText("Payee / biller").assertIsDisplayed()
        composeRule.onNodeWithText("Bill amount").assertIsDisplayed()
        composeRule.onNodeWithText("Type").assertIsDisplayed()
        composeRule.onNodeWithText("Due date").assertIsDisplayed()

        scrollTo("Category")
        composeRule.onNodeWithText("Category").assertIsDisplayed()

        scrollTo("Bill type")
        composeRule.onNodeWithText("Bill type").assertIsDisplayed()

        scrollTo("Save Bill")
        composeRule.onNodeWithText("Save Bill").assertIsDisplayed()
    }

    @Test
    fun enteringPayeeUpdatesFormState() {
        setContent()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Payee / biller").performTextInput("Meralco")
        composeRule.waitForIdle()

        assertEquals("Meralco", viewModel.uiState.value.form.payee)
    }

    @Test
    fun savingWithoutPayeeShowsError() {
        setContent()
        composeRule.waitForIdle()

        scrollTo("Save Bill")
        composeRule.onNodeWithText("Save Bill").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Please enter a payee or biller"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        coVerify(exactly = 0) { addBillUseCase(any()) }
    }

    @Test
    fun savingNewBillCallsAddUseCase() {
        setContent()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Payee / biller").performTextInput("Meralco")
        composeRule.onNodeWithText("Bill amount").performTextInput("900")

        scrollTo("Save Bill")
        composeRule.onNodeWithText("Save Bill").performClick()

        coVerify(timeout = 5_000) {
            addBillUseCase(
                match {
                    it.name == "Meralco" &&
                        it.payee == "Meralco" &&
                        it.amount == 900.0
                }
            )
        }
    }

    // --------------------------------------------------
    // Dropdowns
    // --------------------------------------------------

    @Test
    fun categoryDropdownSelectsCategory() {
        setContent()
        composeRule.waitForIdle()

        scrollTo("Category")
        composeRule.onNodeWithText("Category").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Utilities").performClick()
        composeRule.waitForIdle()

        assertEquals(1L, viewModel.uiState.value.form.categoryId)
        composeRule.onNodeWithText("Utilities").assertIsDisplayed()
    }

    @Test
    fun paymentMethodDropdownSelectsMethod() {
        setContent()
        composeRule.waitForIdle()

        scrollTo("Payment method")
        composeRule.onNodeWithText("Payment method").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("E-Wallet").performClick()
        composeRule.waitForIdle()

        assertEquals(PaymentMethod.E_WALLET, viewModel.uiState.value.form.paymentMethod)
        composeRule.onNodeWithText("E-Wallet").assertIsDisplayed()
    }

    @Test
    fun remindersDropdownTogglesDays() {
        setContent()
        composeRule.waitForIdle()

        scrollTo("Reminders")
        composeRule.onNodeWithText("Reminders").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("7 days before").performClick()
        composeRule.waitForIdle()

        assertTrue(viewModel.uiState.value.form.reminderDays.contains(7))
    }

    // --------------------------------------------------
    // Edit form
    // --------------------------------------------------

    @Test
    fun editFormShowsExistingBillAndActions() {
        coEvery { getBillUseCase(1L) } returns bill()

        setContent(billId = 1L)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.form.payee == "Meralco"
        }

        composeRule.onNodeWithText("Edit Bill").assertIsDisplayed()
        composeRule.onNodeWithText("Meralco").assertIsDisplayed()

        scrollTo("Update Bill")
        composeRule.onNodeWithText("Update Bill").assertIsDisplayed()

        scrollTo("Delete")
        composeRule.onNodeWithText("Skip").assertIsDisplayed()
        composeRule.onNodeWithText("Delete").assertIsDisplayed()
    }

    @Test
    fun deleteShowsConfirmationDialog() {
        coEvery { getBillUseCase(1L) } returns bill()

        setContent(billId = 1L)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.form.canSkip
        }

        scrollTo("Delete")
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Delete bill?").assertIsDisplayed()
        composeRule.onNodeWithText(
            "This bill and its payment history will be permanently removed."
        ).assertIsDisplayed()
    }
}
