package com.smartexpene.categories.state

import com.smartexpense.domain.model.Category

data class CategoryUiState(
    val categories: List<Category> = emptyList(),
    val name: String = "",
    val editingCategoryId: Long? = null,
    val isLoading : Boolean = false,
    val error : String? = null
)