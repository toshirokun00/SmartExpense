package com.smartexpense.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.smartexpense.database.dao.BudgetDao
import com.smartexpense.database.dao.CategoryDao
import com.smartexpense.database.dao.ExpenseDao
import com.smartexpense.database.entity.BudgetEntity
import com.smartexpense.database.entity.CategoryEntity
import com.smartexpense.database.entity.ExpenseEntity

@Database(
    entities = [ExpenseEntity::class, CategoryEntity:: class, BudgetEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao

    abstract fun categoryDao() : CategoryDao

    abstract fun budgetDao(): BudgetDao
}