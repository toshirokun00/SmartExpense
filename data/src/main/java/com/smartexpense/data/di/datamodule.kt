package com.smartexpense.data.di

import com.smartexpense.data.repositoryimpl.CategoryRepositoryImpl
import com.smartexpense.data.repositoryimpl.ExpenseRepositoryImpl
import com.smartexpense.domain.repository.CategoryRepository
import com.smartexpense.domain.repository.ExpenseRepository
import org.koin.dsl.module

val dataModule = module {
    single<ExpenseRepository> {
        ExpenseRepositoryImpl(get())
    }

    single<CategoryRepository> {
        CategoryRepositoryImpl(get())
    }
}