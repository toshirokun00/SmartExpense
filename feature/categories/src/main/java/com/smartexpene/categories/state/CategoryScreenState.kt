package com.smartexpene.categories.state

data class CategoryScreenState(
    val showDeleteConfirmation: Boolean = false,
    val categoryToDeleteId : Long? = null
)