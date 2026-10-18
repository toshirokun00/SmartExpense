package com.smartexpense.feature.expenses.state

import com.smartexpense.domain.model.SortOrder

data class FilterState(
    val isSectionExpanded: Boolean = false,
    val tempQuery: String = "",
    val tempSelectedCategoryIds: Set<Long> = emptySet(),
    val tempStartTime: Long? = null,
    val tempEndTime: Long? = null,
    val tempMinAmount: Double? = null,
    val tempMaxAmount: Double? = null,
    val tempSortOrder: SortOrder = SortOrder.NEWEST
)
