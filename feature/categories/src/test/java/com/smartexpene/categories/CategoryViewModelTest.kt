package com.smartexpene.categories

import app.cash.turbine.test
import com.smartexpene.categories.effect.CategoryEffect
import com.smartexpene.categories.intent.CategoryIntent
import com.smartexpene.categories.viewmodel.CategoryViewModel
import com.smartexpense.domain.model.Category
import com.smartexpense.domain.usecase.CategoryUseCase
import com.smartexpense.test.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CategoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var categoryUseCase : CategoryUseCase
    private lateinit var viewModel : CategoryViewModel


    @Before
    fun setup() {
        categoryUseCase = mockk()

        every {
            categoryUseCase.getCategories()
        } returns flowOf(emptyList())

        viewModel = CategoryViewModel(categoryUseCase)
    }

    @Test
    fun categoriesAreLoadedIntoUiState() = runTest {
        val categories = listOf(
            Category(1L, "Food"),
            Category(2L, "Transport")
        )
        every {
            categoryUseCase.getCategories()
        } returns flowOf(categories)

        viewModel = CategoryViewModel(categoryUseCase)

        viewModel.uiState.test {
            awaitItem()
            val state = awaitItem()

            assertEquals(
                categories, state.categories
            )
        }
    }

    @Test
    fun nameChangedUpdatesUiState() = runTest {
        viewModel.uiState.test {
            awaitItem()

            viewModel.onIntent(
                CategoryIntent.NameChanged("Food")
            )

            val state = awaitItem()

            assertEquals("Food", state.name)
        }
    }

    @Test
    fun addCategoryCallUseCase() = runTest {
        coEvery {
            categoryUseCase.addCategory(any())
        } just Runs

        viewModel.onIntent(CategoryIntent.NameChanged("Food"))


        viewModel.onIntent(CategoryIntent.AddCategory)

        advanceUntilIdle()

        coVerify {
            categoryUseCase.addCategory(
                match {
                 it.name == "Food"
                }
            )
        }
    }

    @Test
    fun addCategoryWithBlankNameSpaceShowsError() = runTest {
        viewModel.effect.test {
            viewModel.onIntent(
                CategoryIntent.AddCategory
            )

            val effect = awaitItem()

            assertEquals(
                CategoryEffect.ShowError(
                    "Please enter category name"
                ),
                effect
            )
        }
    }

    @Test
    fun requestDeleteShowsConfirmation() = runTest {
        viewModel.onIntent(
            CategoryIntent.RequestDeleteCategory(1L)
        )

        val state = viewModel.screenState.value

        assertTrue(state.showDeleteConfirmation)

        assertEquals(1L,state.categoryToDeleteId)

    }

    @Test
    fun confirmDeleteCallsUseCase() = runTest {

        val category = Category(id = 1L, name =  "Food")

        coEvery {
            categoryUseCase.getCategoryById(1L)
        } returns category

        coEvery {
            categoryUseCase.deleteCategory(category)
        } just Runs

        viewModel.onIntent(
            CategoryIntent.RequestDeleteCategory(1L)
        )

        viewModel.onIntent(
            CategoryIntent.ConfirmDeleteCategory
        )
        advanceUntilIdle()

        coVerify {
            categoryUseCase.deleteCategory(category)
        }


    }

    @Test
    fun loadCategoryPopulatesForm() = runTest {
        val category = Category(
            id = 1L,
            name = "Food"
        )

        coEvery {
            categoryUseCase.getCategoryById(1L)
        } returns category

        viewModel.uiState.test {
            awaitItem()

            viewModel.onIntent(
                CategoryIntent.LoadCategory(1L)
            )
            advanceUntilIdle()
            val state = awaitItem()

            assertEquals("Food", state.name)
            assertEquals(1L, state.editingCategoryId)

            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) {
            categoryUseCase.getCategoryById(1L)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun updateCategoryCallsUseCase() = runTest {
        val category = Category(
            id = 1L,
            name = "Food"
        )

        val updatedCategory = Category(
            id = 1L,
            name = "Groceries"
        )

        coEvery {
            categoryUseCase.getCategoryById(1L)
        } returns category

        coEvery {
            categoryUseCase.updateCategory(updatedCategory)
        } just Runs

        viewModel.uiState.test {
            awaitItem()

            // Enter edit mode
            viewModel.onIntent(
                CategoryIntent.LoadCategory(1L)
            )

            advanceUntilIdle()
            awaitItem()

            // Change the name
            viewModel.onIntent(
                CategoryIntent.NameChanged("Groceries")
            )

            awaitItem()

            // Update
            viewModel.onIntent(
                CategoryIntent.UpdateCategory
            )

            advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify {
            categoryUseCase.updateCategory( updatedCategory)
        }
    }

    @Test
    fun updateCategoryWithBlankNameShowsError() = runTest {
        val category = Category(
            id = 1L,
            name = "Food"
        )

        coEvery {
            categoryUseCase.getCategoryById(1L)
        } returns category

        viewModel.onIntent(
            CategoryIntent.LoadCategory(1L)
        )

        advanceUntilIdle()

        viewModel.onIntent(
            CategoryIntent.NameChanged("   ")
        )

        viewModel.effect.test {
            viewModel.onIntent(
                CategoryIntent.UpdateCategory
            )

            advanceUntilIdle()

            val effect = awaitItem()

            assertEquals(
                CategoryEffect.ShowError(
                    "Please enter category name"
                ),
                effect
            )
        }
        coVerify(exactly = 0) {
            categoryUseCase.updateCategory(any())
        }
    }

    @Test
    fun cancelEditClearsForm() = runTest {
        val category = Category(
            id = 1L,
            name = "Food"
        )

        coEvery {
            categoryUseCase.getCategoryById(1L)
        } returns category

        viewModel.uiState.test {
            awaitItem()

            viewModel.onIntent(
                CategoryIntent.LoadCategory(1L)
            )

            awaitItem()

            viewModel.onIntent(
                CategoryIntent.CancelEdit
            )

            val state = awaitItem()

            assertEquals("", state.name)
            assertEquals(null, state.editingCategoryId)
        }
    }

    @Test
    fun loadCategoryWhenCategoryDoesNotExistDoesNotEnterEditMode() = runTest {
        coEvery {
            categoryUseCase.getCategoryById(999L)
        } returns null

        viewModel.onIntent(
            CategoryIntent.LoadCategory(999L)
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals("", state.name)
        assertEquals(null, state.editingCategoryId)
    }

    @Test
    fun loadCategoryCallsUseCase() = runTest {
        val category = Category(
            id = 1L,
            name = "Food"
        )

        coEvery {
            categoryUseCase.getCategoryById(1L)
        } returns category

        viewModel.onIntent(
            CategoryIntent.LoadCategory(1L)
        )

        advanceUntilIdle()

        coVerify(exactly = 1) {
            categoryUseCase.getCategoryById(1L)
        }
    }



}