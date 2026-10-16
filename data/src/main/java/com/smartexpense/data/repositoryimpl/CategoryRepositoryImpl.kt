package com.smartexpense.data.repositoryimpl

import android.util.Log
import com.smartexpense.data.mapper.toDomain
import com.smartexpense.data.mapper.toEntity
import com.smartexpense.data.remote.SupabaseCategoryDto
import com.smartexpense.data.remote.SupabaseCategoryRemoteDataSource
import com.smartexpense.data.sync.ExpenseSyncEnqueuer
import com.smartexpense.database.dao.CategoryDao
import com.smartexpense.domain.model.Category
import com.smartexpense.domain.repository.CategoryRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.coroutines.cancellation.CancellationException

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val supabaseCategoryRemoteDataSource: SupabaseCategoryRemoteDataSource,
    private val supabaseClient: SupabaseClient,
    private val expenseSyncEnqueuer: ExpenseSyncEnqueuer
): CategoryRepository {
    override fun getCategories(): Flow<List<Category>> {
        return categoryDao.getCategories().map { category ->
            category.map {
                it.toDomain()
            }
        }
    }

    override suspend fun getCategoriesById(id: Long): Category? {
        return categoryDao.getCategoryById(id)?.toDomain()
    }

    override suspend fun addCategory(category: Category) {
        categoryDao.insertCategory(category.toEntity())
        expenseSyncEnqueuer.enqueue()
    }

    override suspend fun updateCategory(category: Category) {
        val existingEntity = categoryDao.getCategoryById(category.id)
            ?: return

        categoryDao.updateCategory(
            category.toEntity().copy(
                clientId = existingEntity.clientId
            )
        )
    }

    override suspend fun deleteCategory(category: Category) {
        val existingEntity = categoryDao.getCategoryById(category.id)
            ?: return

        categoryDao.deleteCategory(existingEntity)
    }

    override suspend fun syncCategories() {
        val userId = supabaseClient.auth.currentUserOrNull()?.id ?: return

        val categories = categoryDao.getAllCategoriesForSync()
        var hasFailures = false

        for (category in categories) {
            try {
                val dto =   SupabaseCategoryDto(
                    clientId = category.clientId,
                    userId = userId,
                    name = category.name
                )
                supabaseCategoryRemoteDataSource.upsertCategory(dto)

            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                hasFailures = true
                Log.e(
                    "CategorySync",
                    "Failed to sync category id=${category.id}, name=${category.name}",
                    exception
                )
            }
        }

        if (hasFailures) {
            throw IllegalStateException(
                "Some categories failed to synchronize"
            )
        }
    }


    override suspend fun getRemoteCategoryId(
        localCategoryId: Long
    ): Long? {
        val localCategory = categoryDao.getCategoryById(localCategoryId)
            ?: return null

        val userId = supabaseClient.auth.currentUserOrNull()?.id
            ?: return null

        val remoteCategory = supabaseCategoryRemoteDataSource.upsertCategory(
            SupabaseCategoryDto(
                clientId = localCategory.clientId,
                userId = userId,
                name = localCategory.name
            )
        )

        return remoteCategory.id
    }
}