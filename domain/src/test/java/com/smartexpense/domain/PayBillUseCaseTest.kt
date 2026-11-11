package com.smartexpense.domain

import com.smartexpense.domain.model.Bill
import com.smartexpense.domain.model.BillAmountType
import com.smartexpense.domain.model.BillFrequency
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.model.PaymentMethod
import com.smartexpense.domain.repository.BillRepository
import com.smartexpense.domain.usecase.AddExpenseUseCase
import com.smartexpense.domain.usecase.PayBillUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PayBillUseCaseTest {

    private lateinit var billRepository: BillRepository
    private lateinit var addExpenseUseCase: AddExpenseUseCase
    private lateinit var payBillUseCase: PayBillUseCase

    private val bill = Bill(
        id = 7L,
        name = "Meralco",
        amount = 2400.0,
        dueDateMillis = 1_000L,
        amountType = BillAmountType.VARIABLE,
        payee = "Meralco",
        frequency = BillFrequency.MONTHLY
    )

    @Before
    fun setup() {
        billRepository = mockk(relaxed = true)
        addExpenseUseCase = mockk(relaxed = true)
        payBillUseCase = PayBillUseCase(billRepository, addExpenseUseCase)
    }

    @Test
    fun payingCreatesExpenseAndRecordsPayment() = runTest {
        val expense = slot<Expense>()
        coEvery { addExpenseUseCase.invoke(capture(expense)) } returns Unit

        payBillUseCase(
            bill = bill,
            amountPaid = 2550.0,
            paidAtMillis = 2_000L,
            paymentMethod = PaymentMethod.E_WALLET,
            createExpense = true,
            expenseCategoryId = 3L
        )

        assertEquals("Meralco", expense.captured.description)
        assertEquals(2550.0, expense.captured.amount, 0.0)
        assertEquals(3L, expense.captured.categoryId)
        assertEquals("Meralco", expense.captured.merchant)
        assertEquals(PaymentMethod.E_WALLET, expense.captured.paymentMethod)

        coVerify {
            billRepository.payBill(7L, 2550.0, 2_000L, PaymentMethod.E_WALLET)
        }
    }

    @Test
    fun createExpenseOffStillRecordsPayment() = runTest {
        payBillUseCase(
            bill = bill,
            amountPaid = 2550.0,
            paidAtMillis = 2_000L,
            paymentMethod = PaymentMethod.CASH,
            createExpense = false,
            expenseCategoryId = 3L
        )

        coVerify(exactly = 0) { addExpenseUseCase.invoke(any()) }
        coVerify {
            billRepository.payBill(7L, 2550.0, 2_000L, PaymentMethod.CASH)
        }
    }
}
