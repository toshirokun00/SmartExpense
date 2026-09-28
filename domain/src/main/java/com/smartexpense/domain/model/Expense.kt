package com.smartexpense.domain.model

data class Expense(
    val id : Long = 0L,
    val amount: Double,
    val categoryId : Long,
    val description: String,
    val date: Long

)