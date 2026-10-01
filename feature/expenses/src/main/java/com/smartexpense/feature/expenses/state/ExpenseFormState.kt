package com.smartexpense.feature.expenses.state

data class ExpenseFormState(
    val amount : String = "",
    val description : String = "",
    val categoryId : Long? = null
)
