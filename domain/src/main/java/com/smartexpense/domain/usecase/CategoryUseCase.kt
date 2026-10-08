package com.smartexpense.domain.usecase

import com.smartexpense.domain.model.Category
import com.smartexpense.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow

class CategoryUseCase(
    private val repository: CategoryRepository
) {

    fun getCategories() : Flow<List<Category>> = repository.getCategories()

    suspend fun getCategoryById(id : Long) : Category? = repository.getCategoriesById(id)

    suspend fun addCategory(category: Category) {
        repository.addCategory(category)
    }

    suspend fun updateCategory(category: Category) {
        repository.updateCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        repository.deleteCategory(category)
    }


}