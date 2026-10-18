package com.smartexpense.feature.expenses.state

import com.smartexpense.domain.model.Category
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.model.SortOrder

data class TransactionUiState(
    val expenses: List<Expense> = emptyList(),
    val categories: List<Category> = emptyList(),
    val query: String = "",
    val selectedCategoryIds: Set<Long> = emptySet(),
    val startTime: Long? = null,
    val endTime: Long? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val sortOrder: SortOrder = SortOrder.NEWEST,
    val filter: FilterState = FilterState(),
    val isLoading: Boolean = false,
    val error: String? = null
)
