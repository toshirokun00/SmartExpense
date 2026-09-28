package com.smartexpense.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.smartexpense.database.dao.ExpenseDao
import com.smartexpense.database.entity.ExpenseEntity

@Database(
    entities = [ExpenseEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
}