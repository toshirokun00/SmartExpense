package com.smartexpense.database.di

import androidx.room.Room
import com.smartexpense.database.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single<AppDatabase> {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "smart_expense.db"
        ).build()
    }

    single {
        get<AppDatabase>().expenseDao()
    }

    single {
        get<AppDatabase>().categoryDao()
    }

    single { get<AppDatabase>().budgetDao() }


}