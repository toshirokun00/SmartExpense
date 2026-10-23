package com.smartexpense.data.repositoryimpl

import com.smartexpense.data.mapper.toEntity
import com.smartexpense.data.mapper.toDomain
import com.smartexpense.database.dao.BudgetAlertDao
import com.smartexpense.domain.model.BudgetAlertRecord
import com.smartexpense.domain.repository.BudgetAlertRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BudgetAlertRepositoryImpl(
    private val budgetAlertDao: BudgetAlertDao
) : BudgetAlertRepository {

    override fun getDeliveredAlerts(): Flow<List<BudgetAlertRecord>> {
        return budgetAlertDao.getRecords()
            .map { records ->
                records.map { it.toDomain() }
            }
    }

    override suspend fun recordDeliveredAlerts(records: List<BudgetAlertRecord>) {
        if (records.isEmpty()) return
        budgetAlertDao.insertRecords(records.map { it.toEntity() })
    }

    override suspend fun removeDeliveredAlerts(records: List<BudgetAlertRecord>) {
        records.forEach { record ->
            budgetAlertDao.deleteRecord(
                budgetId = record.budgetId,
                periodStart = record.periodStart,
                periodEnd = record.periodEnd,
                threshold = record.threshold
            )
        }
    }
}
