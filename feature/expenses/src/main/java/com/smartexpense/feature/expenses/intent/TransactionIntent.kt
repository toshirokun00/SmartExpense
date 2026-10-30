package com.smartexpense.feature.expenses.intent

import com.smartexpense.domain.model.SortOrder

sealed interface TransactionIntent {

    data class SearchChanged(val query: String) : TransactionIntent

    data class CategorySelectionChanged(val selectedCategoryIds: Set<Long>) : TransactionIntent

    data class DateRangeChanged(
        val startTime: Long?,
        val endTime: Long?
    ) : TransactionIntent

    data class AmountRangeChanged(
        val minAmount: Double?,
        val maxAmount: Double?
    ) : TransactionIntent

    data class SortChanged(val sortOrder: SortOrder) : TransactionIntent

    data class RetrySync(val id: Long) : TransactionIntent

    object ClearFilters : TransactionIntent

    // Temporary filter state intents (uncommitted until applied)
    object ToggleFilterSection : TransactionIntent

    data class TempCategorySelectionChanged(val selectedCategoryIds: Set<Long>) : TransactionIntent

    data class TempDateRangeChanged(
        val startTime: Long?,
        val endTime: Long?
    ) : TransactionIntent

    data class TempAmountRangeChanged(
        val minAmount: Double?,
        val maxAmount: Double?
    ) : TransactionIntent

    data class TempSortChanged(val sortOrder: SortOrder) : TransactionIntent

    data class TempSearchChanged(val query: String) : TransactionIntent

    object ApplyFilters : TransactionIntent

    object CancelFilters : TransactionIntent
}
