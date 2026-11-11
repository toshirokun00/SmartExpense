package com.smartexpense.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.smartexpense.database.dao.BillDao
import com.smartexpense.database.dao.BillPaymentDao
import com.smartexpense.database.dao.BudgetAlertDao
import com.smartexpense.database.dao.BudgetDao
import com.smartexpense.database.dao.CategoryDao
import com.smartexpense.database.dao.ExpenseDao
import com.smartexpense.database.dao.PendingBillDeletionDao
import com.smartexpense.database.dao.PendingBudgetDeletionDao
import com.smartexpense.database.dao.PendingExpenseDeletionDao
import com.smartexpense.database.dao.RecurringScheduleDao
import com.smartexpense.database.dao.UserProfileDao
import com.smartexpense.database.entity.BillEntity
import com.smartexpense.database.entity.BillPaymentEntity
import com.smartexpense.database.entity.BudgetAlertRecordEntity
import com.smartexpense.database.entity.BudgetEntity
import com.smartexpense.database.entity.CategoryEntity
import com.smartexpense.database.entity.ExpenseEntity
import com.smartexpense.database.entity.PendingBillDeletionEntity
import com.smartexpense.database.entity.PendingBudgetDeletionEntity
import com.smartexpense.database.entity.PendingExpenseDeletionEntity
import com.smartexpense.database.entity.RecurringScheduleEntity
import com.smartexpense.database.entity.UserProfileEntity

@Database(
    entities = [
        ExpenseEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        PendingExpenseDeletionEntity::class,
        PendingBudgetDeletionEntity::class,
        BudgetAlertRecordEntity::class,
        RecurringScheduleEntity::class,
        UserProfileEntity::class,
        BillEntity::class,
        BillPaymentEntity::class,
        PendingBillDeletionEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(SyncStatusConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao

    abstract fun categoryDao() : CategoryDao

    abstract fun budgetDao(): BudgetDao

    abstract fun pendingExpenseDeletionDao(): PendingExpenseDeletionDao

    abstract fun pendingBudgetDeletionDao(): PendingBudgetDeletionDao

    abstract fun budgetAlertDao(): BudgetAlertDao

    abstract fun recurringScheduleDao(): RecurringScheduleDao

    abstract fun userProfileDao(): UserProfileDao

    abstract fun billDao(): BillDao

    abstract fun billPaymentDao(): BillPaymentDao

    abstract fun pendingBillDeletionDao(): PendingBillDeletionDao

    companion object {
    }
}
