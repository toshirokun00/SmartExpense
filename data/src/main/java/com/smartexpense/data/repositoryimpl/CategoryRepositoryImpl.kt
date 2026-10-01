package com.smartexpense.data.repositoryimpl

import com.smartexpense.data.mapper.toDomain
import com.smartexpense.data.mapper.toEntity
import com.smartexpense.database.dao.CategoryDao
import com.smartexpense.domain.model.Category
import com.smartexpense.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao
): CategoryRepository {
    override fun getCategories(): Flow<List<Category>> {
        return categoryDao.getCategories().map { category ->
            category.map {
                it.toDomain()
            }
        }
    }

    override suspend fun getCategoriesById(id: Long): Category? {
       return categoryDao.getCategoryById(id).toDomain()
    }

    override suspend fun addCategory(category: Category) {
       categoryDao.insertCategory(category.toEntity())
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category.toEntity())
    }

    override suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category.toEntity())
    }
}