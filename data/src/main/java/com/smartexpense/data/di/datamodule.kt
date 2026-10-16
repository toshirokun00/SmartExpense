package com.smartexpense.data.di

import com.smartexpense.data.remote.SupabaseCategoryRemoteDataSource
import com.smartexpense.data.remote.SupabaseExpenseRemoteDataSource
import com.smartexpense.data.repositoryimpl.BudgetRepositoryImpl
import com.smartexpense.data.repositoryimpl.CategoryRepositoryImpl
import com.smartexpense.data.repositoryimpl.ExpenseRepositoryImpl
import com.smartexpense.data.repositoryimpl.SettingsRepositoryImpl
import com.smartexpense.data.sync.ExpenseSyncEnqueuer
import com.smartexpense.data.sync.ExpenseSyncEnqueuerImpl
import com.smartexpense.data.sync.ExpenseSyncScheduler
import com.smartexpense.data.sync.ExpenseSyncWorkerFactory
import com.smartexpense.data.sync.WorkManagerExpenseSyncEnqueuer
import com.smartexpense.domain.repository.BudgetRepository
import com.smartexpense.domain.repository.CategoryRepository
import com.smartexpense.domain.repository.ExpenseRepository
import com.smartexpense.domain.repository.SettingsRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single {
        ExpenseSyncScheduler(androidContext())
    }

    single<ExpenseSyncEnqueuer> {
        ExpenseSyncEnqueuerImpl(
            scheduler = get()
        )
    }

    single {
        ExpenseSyncWorkerFactory(
            expenseRepository = get()
        )
    }

    single {
        SupabaseExpenseRemoteDataSource(
            supabaseClient = get()
        )
    }

    single {
        SupabaseCategoryRemoteDataSource(
            supabaseClient = get()
        )
    }

    single<ExpenseRepository> {
        ExpenseRepositoryImpl(
            expenseDao = get(),
            supabaseExpenseRemoteDataSource = get(),
            supabaseClient = get(),
            categoryRepository = get(),
            expenseSyncEnqueuer = get(),
            pendingExpenseDeletionDao = get(),
            deletionTransaction = get()
        )
    }

    single<CategoryRepository> {
        CategoryRepositoryImpl(
            categoryDao = get(),
            supabaseCategoryRemoteDataSource = get(),
            supabaseClient = get(),
            expenseSyncEnqueuer = get()
        )
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
