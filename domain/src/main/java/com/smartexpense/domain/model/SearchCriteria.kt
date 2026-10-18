package com.smartexpense.domain.model

data class SearchCriteria(
    val query: String = "",
    val selectedCategoryIds: Set<Long> = emptySet(),
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val sortOrder: SortOrder = SortOrder.NEWEST
) {
    fun normalize(): SearchCriteria {
        return copy(
            query = query.trim(),
            selectedCategoryIds = selectedCategoryIds.filter { it > 0 }.toSet()
        )
    }
}
