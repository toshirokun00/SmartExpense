package com.smartexpense.data

import android.content.Context
import com.smartexpense.data.remote.SupabaseCategoryRemoteDataSource
import com.smartexpense.data.repositoryimpl.CategoryRepositoryImpl
import com.smartexpense.data.sync.ExpenseSyncEnqueuer
import com.smartexpense.database.dao.CategoryDao
import com.smartexpense.database.entity.CategoryEntity
import com.smartexpense.domain.model.Category
import io.github.jan.supabase.SupabaseClient
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CategoryRepositoryImplTest {

    private lateinit var categoryDao: CategoryDao
    private lateinit var supabaseCategoryRemoteDataSource: SupabaseCategoryRemoteDataSource
    private lateinit var supabaseClient: SupabaseClient
    private val expenseSyncEnqueuer =
        mockk<ExpenseSyncEnqueuer>(relaxed = true)
    private lateinit var repository: CategoryRepositoryImpl

    @Before
    fun setup(){
        categoryDao = mockk()
        supabaseCategoryRemoteDataSource = mockk()
        supabaseClient = mockk()

        repository = CategoryRepositoryImpl(
            categoryDao = categoryDao,
            supabaseCategoryRemoteDataSource = supabaseCategoryRemoteDataSource,
            supabaseClient = supabaseClient,
           expenseSyncEnqueuer = expenseSyncEnqueuer
        )
    }

    @Test
    fun getCategoriesMapEntitiesToDomain() = runTest {
        val entities = listOf(
            CategoryEntity(
                id =  1L,
                name = "Food"
            ),
            CategoryEntity(
                id =  2L,
                name = "Transport"
            )
        )

        every {
            categoryDao.getCategories()
        } returns flowOf(entities)


        val result = repository.getCategories().first()

        assertEquals(
            listOf(
                Category(1L, "Food"),
                Category(2L, "Transport")
            ),
            result
        )
    }


    @Test
    fun getCategoryByIdMapsEntityToDomain() = runTest {
        val entity = CategoryEntity(
            id = 1L,
            name = "Food",
            clientId = "test-client-id"
        )

        coEvery {
            categoryDao.getCategoryById(1L)
        } returns entity

        val result = repository.getCategoriesById(1L)

        assertEquals(
            Category(id = 1L, name = "Food"),
            result
        )

        coVerify {
            categoryDao.getCategoryById(1L)
        }
    }



    @Test
    fun updateCategoryMapsDomainEntity() = runTest {
        val category = Category(
            id = 1L,
            name = "Updated Food"
        )

        coEvery {
            categoryDao.getCategoryById(1L)
        } returns CategoryEntity(
            id = 1L,
            name = "Food",
            clientId = "test-client-id"
        )

        coEvery {
            categoryDao.updateCategory(any())
        } just Runs

        repository.updateCategory(category)

        coVerify {
            categoryDao.getCategoryById(1L)
        }

        coVerify {
            categoryDao.updateCategory(
                match {
                    it.id == 1L &&
                            it.name == "Updated Food" &&
                            it.clientId == "test-client-id"
                }
            )
        }
    }



    @Test
    fun deleteCategoryMapsDomainToEntity() = runTest {
        val category = Category(
            id = 1L,
            name = "Food"
        )

        coEvery {
            categoryDao.getCategoryById(1L)
        } returns CategoryEntity(
            id = 1L,
            name = "Food",
            clientId = "test-client-id"
        )

        coEvery {
            categoryDao.deleteCategory(any())
        } just Runs

        repository.deleteCategory(category)

        coVerify {
            categoryDao.getCategoryById(1L)
        }

        coVerify {
            categoryDao.deleteCategory(
                match {
                    it.id == 1L &&
                            it.name == "Food" &&
                            it.clientId == "test-client-id"
                }
            )
        }
    }


}