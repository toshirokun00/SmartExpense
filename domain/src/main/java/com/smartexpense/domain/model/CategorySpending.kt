package com.smartexpense.domain.model

data class CategorySpending(
    val categoryId: Long,
    val totalAmount: Double,
    val percentage: Double
)