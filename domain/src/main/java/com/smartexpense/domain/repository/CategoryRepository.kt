package com.smartexpense.domain.repository

import com.smartexpense.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    fun getCategories() : Flow<List<Category>>

    suspend fun getCategoriesById(id : Long) : Category?

    suspend fun addCategory(category: Category)

    suspend fun updateCategory(category: Category)

    suspend fun deleteCategory(category: Category)
}