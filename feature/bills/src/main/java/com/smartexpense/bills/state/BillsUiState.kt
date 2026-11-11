package com.smartexpense.bills.state

import com.smartexpense.domain.model.Bill
import com.smartexpense.domain.model.Category

data class BillsUiState(
    val bills: List<Bill> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val payingBill: Bill? = null
)
