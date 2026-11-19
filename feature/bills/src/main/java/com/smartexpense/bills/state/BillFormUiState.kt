package com.smartexpense.bills.state

import com.smartexpense.domain.model.BillAmountType
import com.smartexpense.domain.model.BillFrequency
import com.smartexpense.domain.model.Category
import com.smartexpense.domain.model.PaymentMethod
import java.time.LocalDate
import java.time.ZoneId

/**
 * Today at local midnight. A default due date carries no time of day so the
 * date-only planning math (bill cycles, paycheck coverage) stays aligned.
 */
fun startOfTodayMillis(): Long =
    LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

/** Text-field values and toggles captured by the add/edit bill screen. */
data class BillFormState(
    val amount: String = "",
    val dueDateMillis: Long = startOfTodayMillis(),
    val notes: String = "",
    val isRecurring: Boolean = false,
    val frequency: BillFrequency = BillFrequency.MONTHLY,
    val intervalText: String = "1",
    val amountType: BillAmountType = BillAmountType.STATIC,
    val categoryId: Long? = null,
    val payee: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val autopay: Boolean = false,
    val reminderDays: Set<Int> = setOf(3),
    val reminderTimeMinutes: Int = 9 * 60,
    val isActive: Boolean = true,
    val editingBillId: Long? = null,
    val canSkip: Boolean = false,
    val isSaving: Boolean = false
)

data class BillFormUiState(
    val form: BillFormState = BillFormState(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true
)
