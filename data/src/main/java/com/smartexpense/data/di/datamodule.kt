package com.smartexpense.data.di

import com.smartexpense.data.repositoryimpl.BudgetRepositoryImpl
import com.smartexpense.data.repositoryimpl.CategoryRepositoryImpl
import com.smartexpense.data.repositoryimpl.ExpenseRepositoryImpl
import com.smartexpense.data.repositoryimpl.SettingsRepositoryImpl
import com.smartexpense.domain.repository.BudgetRepository
import com.smartexpense.domain.repository.CategoryRepository
import com.smartexpense.domain.repository.ExpenseRepository
import com.smartexpense.domain.repository.SettingsRepository
import org.koin.dsl.module

val dataModule = module {
    single<ExpenseRepository> {
        ExpenseRepositoryImpl(get())
    }

    single<CategoryRepository> {
        CategoryRepositoryImpl(get())
    }

    single<BudgetRepository> {
        BudgetRepositoryImpl(
            budgetDao = get()
        )
    }

    single<SettingsRepository> {
        SettingsRepositoryImpl(
            dataStore = get()
        )
    }


}