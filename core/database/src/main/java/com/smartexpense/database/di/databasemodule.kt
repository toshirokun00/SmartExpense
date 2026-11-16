package com.smartexpense.database.di

import androidx.room.Room
import com.smartexpense.database.AppDatabase
import com.smartexpense.database.BillDeletionTransaction
import com.smartexpense.database.BudgetDeletionTransaction
import com.smartexpense.database.ExpenseDeletionTransaction
import com.smartexpense.database.MIGRATION_1_2
import com.smartexpense.database.MIGRATION_2_3
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single<AppDatabase> {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "smart_expense.db"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()
    }

    single {
        get<AppDatabase>().expenseDao()
    }

    single {
        get<AppDatabase>().categoryDao()
    }

    single { get<AppDatabase>().budgetDao() }

    single {
        get<AppDatabase>().pendingExpenseDeletionDao()
    }

    single { get<AppDatabase>().pendingBudgetDeletionDao() }

    single { get<AppDatabase>().budgetAlertDao() }

    single { get<AppDatabase>().recurringScheduleDao() }

    single { get<AppDatabase>().userProfileDao() }

    single { get<AppDatabase>().billDao() }

    single { get<AppDatabase>().billPaymentDao() }

    single { get<AppDatabase>().pendingBillDeletionDao() }

    single { get<AppDatabase>().paycheckDao() }

    single { get<AppDatabase>().paycheckBillDao() }

    single { get<AppDatabase>().paycheckScheduleDao() }

    single { get<AppDatabase>().pendingPaycheckDeletionDao() }

    single { ExpenseDeletionTransaction(database = get()) }

    single { BudgetDeletionTransaction(database = get()) }

    single { BillDeletionTransaction(database = get()) }


}
