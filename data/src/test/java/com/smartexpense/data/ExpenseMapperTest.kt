package com.smartexpense.data

import com.smartexpense.data.repositoryimpl.mapper.toDomain
import com.smartexpense.data.repositoryimpl.mapper.toEntity
import com.smartexpense.database.entity.ExpenseEntity
import com.smartexpense.database.entity.SyncStatus
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.model.ExpenseSyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpenseMapperTest {

    @Test
    fun mapsEntityMinorUnitsAndMerchantToDomain() {
        val entity = ExpenseEntity(
            id = 7L,
            amountMinor = 12345L,
            categoryId = 3L,
            description = "Coffee",
            date = 1_760_000_000_000L,
            merchant = "Cafe Uno",
            syncStatus = SyncStatus.FAILED,
            clientId = "client-7"
        )

        val domain = entity.toDomain()

        assertEquals(12345L, domain.amountMinor)
        assertEquals(123.45, domain.amount, 0.0)
        assertEquals("Cafe Uno", domain.merchant)
        assertEquals(ExpenseSyncStatus.FAILED, domain.syncStatus)
    }

    @Test
    fun mapsDomainMinorUnitsAndMerchantToEntity() {
        val expense = Expense(
            id = 7L,
            amount = 123.45,
            categoryId = 3L,
            description = "Coffee",
            date = 1_760_000_000_000L,
            merchant = "Cafe Uno",
            syncStatus = ExpenseSyncStatus.SYNCED
        )

        val entity = expense.toEntity()

        assertEquals(12345L, entity.amountMinor)
        assertEquals("Cafe Uno", entity.merchant)
        assertEquals(SyncStatus.SYNCED, entity.syncStatus)
    }
}
