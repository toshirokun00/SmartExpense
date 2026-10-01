package com.smartexpene.categories.state

data class CategoryFormState(
    val name : String = "",
    val editingCategoryId : Long ? = null
)
