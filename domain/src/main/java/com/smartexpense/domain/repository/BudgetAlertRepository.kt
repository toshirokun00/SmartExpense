package com.smartexpense.domain.repository

import com.smartexpense.domain.model.BudgetAlertRecord
import kotlinx.coroutines.flow.Flow

/**
 * Persistence for budget alert delivery history. Used to prevent duplicate
 * notifications for the same budget, period and threshold.
 */
interface BudgetAlertRepository {

    fun getDeliveredAlerts(): Flow<List<BudgetAlertRecord>>

    suspend fun recordDeliveredAlerts(records: List<BudgetAlertRecord>)

    suspend fun removeDeliveredAlerts(records: List<BudgetAlertRecord>)
}
