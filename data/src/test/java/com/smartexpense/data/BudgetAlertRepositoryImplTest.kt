package com.smartexpense.data

import com.smartexpense.data.repositoryimpl.BudgetAlertRepositoryImpl
import com.smartexpense.database.dao.BudgetAlertDao
import com.smartexpense.database.entity.BudgetAlertRecordEntity
import com.smartexpense.domain.model.BudgetAlertRecord
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetAlertRepositoryImplTest {

    private val dao: BudgetAlertDao = mockk()
    private val repository = BudgetAlertRepositoryImpl(dao)

    @Test
    fun getDeliveredAlertsMapsEntitiesToDomain() = runTest {
        every { dao.getRecords() } returns flowOf(
            listOf(
                BudgetAlertRecordEntity(1L, 10L, 20L, 80),
                BudgetAlertRecordEntity(2L, 30L, 40L, 100)
            )
        )

        val records = repository.getDeliveredAlerts().first()

        assertEquals(
            listOf(
                BudgetAlertRecord(1L, 10L, 20L, 80),
                BudgetAlertRecord(2L, 30L, 40L, 100)
            ),
            records
        )
    }

    @Test
    fun recordDeliveredAlertsInsertsMappedEntities() = runTest {
        val records = listOf(BudgetAlertRecord(1L, 10L, 20L, 80))

        coEvery { dao.insertRecords(any()) } just Runs

        repository.recordDeliveredAlerts(records)

        coVerify(exactly = 1) {
            dao.insertRecords(
                match {
                    it == listOf(BudgetAlertRecordEntity(1L, 10L, 20L, 80))
                }
            )
        }
    }

    @Test
    fun recordDeliveredAlertsWithEmptyListDoesNotHitDatabase() = runTest {
        repository.recordDeliveredAlerts(emptyList())

        coVerify(exactly = 0) { dao.insertRecords(any()) }
    }

    @Test
    fun removeDeliveredAlertsDeletesEachRecord() = runTest {
        coEvery {
            dao.deleteRecord(any(), any(), any(), any())
        } just Runs

        repository.removeDeliveredAlerts(
            listOf(
                BudgetAlertRecord(1L, 10L, 20L, 80),
                BudgetAlertRecord(1L, 10L, 20L, 100)
            )
        )

        coVerify(exactly = 1) { dao.deleteRecord(1L, 10L, 20L, 80) }
        coVerify(exactly = 1) { dao.deleteRecord(1L, 10L, 20L, 100) }
    }
}
