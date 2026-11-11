package com.smartexpense.bills

import com.smartexpense.bills.intent.BillsIntent
import com.smartexpense.bills.viewmodel.BillsViewModel
import com.smartexpense.domain.model.Bill
import com.smartexpense.domain.model.Category
import com.smartexpense.domain.model.PaymentMethod
import com.smartexpense.domain.usecase.CategoryUseCase
import com.smartexpense.domain.usecase.GetBillsUseCase
import com.smartexpense.domain.usecase.PayBillUseCase
import com.smartexpense.test.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
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

class BillsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

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
        categoriesFlow = MutableStateFlow(emptyList())

        every { getBillsUseCase() } returns billsFlow
        every { categoryUseCase.getCategories() } returns categoriesFlow
        coEvery {
            payBillUseCase(
                any(), any(), any(), any(), any(), any()
            )
        } just Runs

        viewModel = createViewModel()
    }

    private fun createViewModel(): BillsViewModel = BillsViewModel(
        getBillsUseCase = getBillsUseCase,
        payBillUseCase = payBillUseCase,
        categoryUseCase = categoryUseCase
    )

    private fun bill(
        id: Long = 1L,
        name: String = "Meralco",
        amount: Double = 900.0,
        categoryId: Long? = 1L
    ) = Bill(
        id = id,
        name = name,
        amount = amount,
        dueDateMillis = DUE_DATE,
        categoryId = categoryId
    )

    // --------------------------------------------------
    // Initial data
    // --------------------------------------------------

    @Test
    fun initCollectsBillsAndClearsLoading() = runTest {
        val bills = listOf(bill())
        billsFlow.value = bills

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(bills, state.bills)
        assertFalse(state.isLoading)
    }

    @Test
    fun initCollectsCategories() = runTest {
        categoriesFlow.value = listOf(Category(id = 1L, name = "Utilities"))

        advanceUntilIdle()

        assertEquals(
            listOf(Category(id = 1L, name = "Utilities")),
            viewModel.uiState.value.categories
        )
    }

    @Test
    fun billUpdatesAreForwardedToState() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.bills.isEmpty())

        billsFlow.value = listOf(bill(id = 2L, name = "Maynilad"))
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.bills.size)
        assertEquals("Maynilad", viewModel.uiState.value.bills.first().name)
    }

    // --------------------------------------------------
    // Pay sheet visibility
    // --------------------------------------------------

    @Test
    fun showPaySheetSelectsBill() = runTest {
        val selected = bill()

        viewModel.onIntent(BillsIntent.ShowPaySheet(selected))

        assertEquals(selected, viewModel.uiState.value.payingBill)
    }

    @Test
    fun hidePaySheetClearsBill() = runTest {
        viewModel.onIntent(BillsIntent.ShowPaySheet(bill()))
        viewModel.onIntent(BillsIntent.HidePaySheet)

        assertNull(viewModel.uiState.value.payingBill)
    }

    // --------------------------------------------------
    // Paying
    // --------------------------------------------------

    @Test
    fun payUsesBillCategory() = runTest {
        val target = bill(categoryId = 5L)
        billsFlow.value = listOf(target)
        categoriesFlow.value = listOf(Category(id = 9L, name = "Other"))
        advanceUntilIdle()

        viewModel.onIntent(
            BillsIntent.Pay(
                id = target.id,
                amount = 950.0,
                paidAtMillis = PAID_AT,
                paymentMethod = PaymentMethod.E_WALLET,
                createExpense = true
            )
        )
        advanceUntilIdle()

        coVerify(exactly = 1) {
            payBillUseCase(
                match { it.id == target.id },
                950.0,
                PAID_AT,
                PaymentMethod.E_WALLET,
                true,
                5L
            )
        }
    }

    @Test
    fun payFallsBackToFirstCategoryWhenBillHasNone() = runTest {
        val target = bill(categoryId = null)
        billsFlow.value = listOf(target)
        categoriesFlow.value = listOf(
            Category(id = 3L, name = "Food"),
            Category(id = 4L, name = "Transport")
        )
        advanceUntilIdle()

        viewModel.onIntent(
            BillsIntent.Pay(
                id = target.id,
                amount = 900.0,
                paidAtMillis = PAID_AT,
                paymentMethod = PaymentMethod.CASH,
                createExpense = true
            )
        )
        advanceUntilIdle()

        coVerify(exactly = 1) {
            payBillUseCase(any(), 900.0, PAID_AT, PaymentMethod.CASH, true, 3L)
        }
    }

    @Test
    fun payFallsBackToZeroCategoryWhenThereAreNoCategories() = runTest {
        val target = bill(categoryId = null)
        billsFlow.value = listOf(target)
        advanceUntilIdle()

        viewModel.onIntent(
            BillsIntent.Pay(
                id = target.id,
                amount = 900.0,
                paidAtMillis = PAID_AT,
                paymentMethod = PaymentMethod.CASH,
                createExpense = false
            )
        )
        advanceUntilIdle()

        coVerify(exactly = 1) {
            payBillUseCase(any(), 900.0, PAID_AT, PaymentMethod.CASH, false, 0L)
        }
    }

    @Test
    fun payUsesPayingBillWhenItIsNotInTheList() = runTest {
        val target = bill(id = 42L)
        viewModel.onIntent(BillsIntent.ShowPaySheet(target))

        viewModel.onIntent(
            BillsIntent.Pay(
                id = 42L,
                amount = 900.0,
                paidAtMillis = PAID_AT,
                paymentMethod = PaymentMethod.CASH,
                createExpense = true
            )
        )
        advanceUntilIdle()

        coVerify(exactly = 1) {
            payBillUseCase(match { it.id == 42L }, any(), any(), any(), any(), any())
        }
    }

    @Test
    fun payUnknownBillDoesNothing() = runTest {
        advanceUntilIdle()

        viewModel.onIntent(
            BillsIntent.Pay(
                id = 99L,
                amount = 900.0,
                paidAtMillis = PAID_AT,
                paymentMethod = PaymentMethod.CASH,
                createExpense = true
            )
        )
        advanceUntilIdle()

        coVerify(exactly = 0) { payBillUseCase(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun payClosesPaySheet() = runTest {
        val target = bill()
        billsFlow.value = listOf(target)
        advanceUntilIdle()
        viewModel.onIntent(BillsIntent.ShowPaySheet(target))

        viewModel.onIntent(
            BillsIntent.Pay(
                id = target.id,
                amount = 900.0,
                paidAtMillis = PAID_AT,
                paymentMethod = PaymentMethod.CASH,
                createExpense = true
            )
        )
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.payingBill)
    }

    companion object {
        private const val DUE_DATE = 1_763_000_000_000L
        private const val PAID_AT = 1_762_000_000_000L
    }
}
