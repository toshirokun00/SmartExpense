package com.smartexpense.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds paycheck planning tables. Existing expenses, budgets, and bills are left
 * untouched so upgrading users keep all of their local data.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `paychecks` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`userId` TEXT NOT NULL, " +
                "`clientId` TEXT NOT NULL, " +
                "`syncStatus` TEXT NOT NULL, " +
                "`amount` REAL NOT NULL, " +
                "`payDate` INTEGER NOT NULL, " +
                "`label` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_paychecks_clientId` " +
                "ON `paychecks` (`clientId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_paychecks_userId_payDate` " +
                "ON `paychecks` (`userId`, `payDate`)"
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `paycheck_bills` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`userId` TEXT NOT NULL, " +
                "`paycheckId` INTEGER NOT NULL, " +
                "`billId` INTEGER NOT NULL, " +
                "`clientId` TEXT NOT NULL, " +
                "`paycheckClientId` TEXT NOT NULL, " +
                "`billClientId` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_paycheck_bills_paycheckId` " +
                "ON `paycheck_bills` (`paycheckId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_paycheck_bills_billId` " +
                "ON `paycheck_bills` (`billId`)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_paycheck_bills_clientId` " +
                "ON `paycheck_bills` (`clientId`)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS " +
                "`index_paycheck_bills_paycheckClientId_billClientId` " +
                "ON `paycheck_bills` (`paycheckClientId`, `billClientId`)"
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `pending_paycheck_deletions` (" +
                "`clientId` TEXT NOT NULL, " +
                "PRIMARY KEY(`clientId`))"
        )
    }
}

/**
 * Adds recurring paycheck schedules and links generated paychecks back to the
 * schedule that created them.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `paychecks` ADD COLUMN `scheduleClientId` TEXT"
        )
        db.execSQL(
            "ALTER TABLE `paychecks` ADD COLUMN `occurrenceDate` INTEGER"
        )
        db.execSQL(
            "ALTER TABLE `paychecks` ADD COLUMN `isSkipped` INTEGER NOT NULL " +
                "DEFAULT 0"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS " +
                "`index_paychecks_scheduleClientId_occurrenceDate` " +
                "ON `paychecks` (`scheduleClientId`, `occurrenceDate`)"
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `paycheck_schedules` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`userId` TEXT NOT NULL, " +
                "`clientId` TEXT NOT NULL, " +
                "`syncStatus` TEXT NOT NULL, " +
                "`amount` REAL NOT NULL, " +
                "`label` TEXT NOT NULL, " +
                "`frequency` TEXT NOT NULL, " +
                "`firstDay` INTEGER NOT NULL, " +
                "`secondDay` INTEGER, " +
                "`startDate` INTEGER NOT NULL, " +
                "`endDate` INTEGER, " +
                "`isActive` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS " +
                "`index_paycheck_schedules_clientId` " +
                "ON `paycheck_schedules` (`clientId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS " +
                "`index_paycheck_schedules_userId_isActive` " +
                "ON `paycheck_schedules` (`userId`, `isActive`)"
        )
    }
}
