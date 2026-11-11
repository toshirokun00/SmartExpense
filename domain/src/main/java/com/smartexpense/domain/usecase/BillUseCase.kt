package com.smartexpense.domain.usecase

import com.smartexpense.domain.model.Bill
import com.smartexpense.domain.model.BillPayment
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.model.ExpenseType
import com.smartexpense.domain.model.PaymentMethod
import com.smartexpense.domain.repository.BillRepository
import kotlinx.coroutines.flow.Flow

class GetBillsUseCase(private val repo: BillRepository) {
    operator fun invoke(): Flow<List<Bill>> = repo.getBills()
}

class GetBillUseCase(private val repo: BillRepository) {
    suspend operator fun invoke(id: Long): Bill? = repo.getBill(id)
}

class GetBillPaymentsUseCase(private val repo: BillRepository) {
    operator fun invoke(billId: Long): Flow<List<BillPayment>> = repo.getPayments(billId)
}

class AddBillUseCase(private val repo: BillRepository) {
    suspend operator fun invoke(bill: Bill): Long = repo.addBill(bill)
}

class UpdateBillUseCase(private val repo: BillRepository) {
    suspend operator fun invoke(bill: Bill) = repo.updateBill(bill)
}

class DeleteBillUseCase(private val repo: BillRepository) {
    suspend operator fun invoke(id: Long) = repo.deleteBill(id)
}

class SkipBillUseCase(private val repo: BillRepository) {
    suspend operator fun invoke(id: Long) = repo.skipBill(id)
}

/**
 * Pays a bill and optionally records a matching expense so the payment shows up
 * in budgets and reports. The expense is written first: if it fails, the bill is
 * left unpaid instead of silently disappearing from spending totals.
 */
class PayBillUseCase(
    private val billRepository: BillRepository,
    private val addExpenseUseCase: AddExpenseUseCase
) {
    suspend operator fun invoke(
        bill: Bill,
        amountPaid: Double,
        paidAtMillis: Long,
        paymentMethod: PaymentMethod,
        createExpense: Boolean,
        expenseCategoryId: Long
    ) {
        if (createExpense) {
            addExpenseUseCase(
                Expense(
                    amount = amountPaid,
                    categoryId = expenseCategoryId,
                    description = bill.name,
                    date = paidAtMillis,
                    merchant = bill.payee,
                    paymentMethod = paymentMethod,
                    expenseType = ExpenseType.EXPENSE,
                    notes = "Bill payment"
                )
            )
        }
        billRepository.payBill(
            id = bill.id,
            amountPaid = amountPaid,
            paidAtMillis = paidAtMillis,
            paymentMethod = paymentMethod
        )
    }
}
