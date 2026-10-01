package com.smartexpene.categories.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartexpene.categories.effect.CategoryEffect
import com.smartexpene.categories.intent.CategoryIntent
import com.smartexpene.categories.state.CategoryFormState
import com.smartexpene.categories.state.CategoryScreenState
import com.smartexpene.categories.state.CategoryUiState
import com.smartexpense.domain.model.Category
import com.smartexpense.domain.usecase.CategoryUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CategoryViewModel(
    private val categoryUseCase: CategoryUseCase
) : ViewModel() {

    private val _formState = MutableStateFlow(CategoryFormState())

    private val _screenState = MutableStateFlow(CategoryScreenState())
    val screenState = _screenState.asStateFlow()

    private val _effect = MutableSharedFlow<CategoryEffect>()
    val effect = _effect.asSharedFlow()

    private val categoriesFlow = categoryUseCase.getCategories()
        .catch { exception ->
            _effect.emit(
                CategoryEffect.ShowError(exception.message ?: "Failed to load categories")
            )

        }

    val uiState: StateFlow<CategoryUiState> = combine(
        categoriesFlow, _formState
    ) { categories, formState ->
        CategoryUiState(
            categories = categories,
            name = formState.name,
            editingCategoryId = formState.editingCategoryId,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Companion.WhileSubscribed(5_000),
        initialValue = CategoryUiState(isLoading = true)
    )


    fun onIntent(intent: CategoryIntent) {
        when (intent) {
            CategoryIntent.AddCategory -> {
                addCategory()
            }

            CategoryIntent.CancelDeleteCategory -> {
                cancelDeleteCategory()
            }

            CategoryIntent.ConfirmDeleteCategory ->
                confirmDeleteCategory()

            is CategoryIntent.LoadCategory -> {
                loadCategory(id = intent.id)

            }

            is CategoryIntent.NameChanged -> {
                _formState.update {
                    it.copy(
                        name =  intent.name
                    )
                }
            }

            is CategoryIntent.RequestDeleteCategory -> {
                requestDeleteCategory(intent.id)
            }

             CategoryIntent.UpdateCategory -> {
                updateCategory()
            }

            CategoryIntent.CancelEdit -> {
                _formState.value = CategoryFormState()
            }
        }
    }


    private fun loadCategory(id: Long) {

        viewModelScope.launch {
            val category = categoryUseCase.getCategoriesById(id)

            if (category == null) {
                _effect.emit(
                    CategoryEffect.ShowError(
                        "Category not found"
                    )
                )
                return@launch
            }

            _formState.value = CategoryFormState(
                name = category.name,
                editingCategoryId = category.id
            )
        }

    }

    private fun updateCategory() {

        val state = _formState.value
        val categoryId = state.editingCategoryId

        val name = state.name.trim()

        if (categoryId == null){
            return
        }

        if (name.isBlank()) {
            viewModelScope.launch {
                _effect.emit(
                    CategoryEffect.ShowError(
                        "Please enter category name"
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            val existingCategory = categoryUseCase.getCategoriesById(categoryId)

            if (existingCategory == null) {
                _effect.emit(
                    CategoryEffect.ShowError(
                        "Category not found"
                    )
                )
                return@launch
            }


            categoryUseCase.updateCategory(existingCategory.copy(name = name))

            _formState.value = CategoryFormState()

            _effect.emit(
                CategoryEffect.CategoryUpdated
            )
        }

    }

    private fun addCategory() {
        val name = _formState.value.name.trim()

        if (name.isBlank()) {
            viewModelScope.launch {
                _effect.emit(
                    CategoryEffect.ShowError(
                        "Please enter category name"
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            categoryUseCase.addCategory(
                Category(name = name)
            )
            _formState.value = CategoryFormState()

            _effect.emit(
                CategoryEffect.CategoryAdded
            )
        }

    }

    private fun requestDeleteCategory(id: Long) {
        _screenState.update {
            it.copy(
                showDeleteConfirmation = true,
                categoryToDeleteId = id
            )
        }
    }

    private fun confirmDeleteCategory() {

        val categoryId = _screenState.value.categoryToDeleteId ?: return

        viewModelScope.launch {
            val category = categoryUseCase.getCategoriesById(id = categoryId)

            if (category == null) {
                _effect.emit(CategoryEffect.ShowError("Category not found"))

                cancelDeleteCategory()
                return@launch
            }

            categoryUseCase.deleteCategory(category)

            _effect.emit(
                CategoryEffect.CategoryDeleted
            )

            cancelDeleteCategory()
        }

    }

    private fun cancelDeleteCategory() {
        _screenState.update {
            it.copy(showDeleteConfirmation = false,
                categoryToDeleteId = null)
        }

    }

}