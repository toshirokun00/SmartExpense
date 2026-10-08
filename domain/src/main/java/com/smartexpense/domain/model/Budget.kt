package com.smartexpense.domain.model

data class Budget(
    val id: Long = 0L,
    val categoryId: Long,
    val amount: Double,
    val startDate: Long,
    val endDate: Long
)