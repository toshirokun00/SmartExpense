package com.smartexpense.domain

import com.smartexpense.domain.model.Category
import com.smartexpense.domain.repository.CategoryRepository
import com.smartexpense.domain.usecase.CategoryUseCase
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CategoryUseCaseTest {

    private lateinit var repository : CategoryRepository
    private lateinit var useCase : CategoryUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = CategoryUseCase(repository)
    }

    @Test
    fun getCategoriesReturnCategoriesFromRepository()  = runTest {
        val categories = listOf(
            Category(id = 1L, name = "Food"),
            Category(id = 2L, name = "Transport")
        )

        every {
            repository.getCategories()
        } returns flowOf(categories)

        val result = useCase.getCategories().first()

        assertEquals(categories, result)

    }

    @Test
    fun getCategoryByIdReturnsCategory() = runTest {
        val category = Category(id = 1L, name = "Food")

        coEvery {
            repository.getCategoriesById(1L)
        } returns category

        val result = useCase.getCategoryById(1L)

        Assert.assertEquals(category, result)

        coVerify {
            repository.getCategoriesById(1L)
        }
    }

    @Test
    fun addCategoryCallsRepository() = runTest {
        val category = Category(
            name = "Food"
        )

        coEvery {
            repository.addCategory(category)
        } just Runs

        useCase.addCategory(category)

        coVerify {
            repository.addCategory(category)
        }

    }

    @Test
    fun updateCategoryCallsRepository() = runTest {
        val category = Category(id = 1L, name = "Food")

        coEvery {
            repository.updateCategory(category)
        } just Runs

        useCase.updateCategory(category)

        coEvery {
            repository.updateCategory(category)
        }

    }

    @Test
    fun deleteCategoryCallRepository() = runTest {
        val category = Category(id = 1L, name = "Food")

        coEvery {
            repository.deleteCategory(category)
        } just Runs

        useCase.deleteCategory(category)

        coVerify {
            repository.deleteCategory(category)
        }
    }

}