package com.smartexpense.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartexpense.database.dao.BudgetAlertDao
import com.smartexpense.database.entity.BudgetAlertRecordEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BudgetAlertDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var budgetAlertDao: BudgetAlertDao

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        budgetAlertDao = database.budgetAlertDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun record(
        budgetId: Long = 1L,
        periodStart: Long = 100L,
        periodEnd: Long = 200L,
        threshold: Int = 80
    ) = BudgetAlertRecordEntity(
        budgetId = budgetId,
        periodStart = periodStart,
        periodEnd = periodEnd,
        threshold = threshold
    )

    @Test
    fun insertAndReadRecords() = runBlocking {
        budgetAlertDao.insertRecords(
            listOf(record(threshold = 80), record(threshold = 100))
        )

        val records = budgetAlertDao.getRecords().first()

        assertEquals(2, records.size)
        assertTrue(records.any { it.threshold == 80 })
        assertTrue(records.any { it.threshold == 100 })
    }

    @Test
    fun duplicateCompositeKeyIsIgnored() = runBlocking {
        budgetAlertDao.insertRecords(listOf(record(threshold = 80)))
        budgetAlertDao.insertRecords(listOf(record(threshold = 80)))

        val records = budgetAlertDao.getRecords().first()

        assertEquals(1, records.size)
    }

    @Test
    fun sameBudgetDifferentPeriodOrThresholdIsDistinct() = runBlocking {
        budgetAlertDao.insertRecords(
            listOf(
                record(periodStart = 100L, periodEnd = 200L, threshold = 80),
                record(periodStart = 200L, periodEnd = 300L, threshold = 80),
                record(periodStart = 100L, periodEnd = 200L, threshold = 100)
            )
        )

        assertEquals(3, budgetAlertDao.getRecords().first().size)
    }

    @Test
    fun deleteRecordRemovesOnlyMatchingRow() = runBlocking {
        budgetAlertDao.insertRecords(
            listOf(record(threshold = 80), record(threshold = 100))
        )

        budgetAlertDao.deleteRecord(
            budgetId = 1L,
            periodStart = 100L,
            periodEnd = 200L,
            threshold = 80
        )

        val records = budgetAlertDao.getRecords().first()

        assertEquals(1, records.size)
        assertEquals(100, records.first().threshold)
    }
}
