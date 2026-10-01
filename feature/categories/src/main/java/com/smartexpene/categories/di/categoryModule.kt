package com.smartexpene.categories.di

import com.smartexpene.categories.viewmodel.CategoryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val categoryModule = module {
    viewModel {
        CategoryViewModel(
            categoryUseCase = get()
        )
    }
}
