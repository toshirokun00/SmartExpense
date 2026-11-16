package com.smartexpense.domain.repository

import com.smartexpense.domain.model.Paycheck
import com.smartexpense.domain.model.PaycheckSchedule
import kotlinx.coroutines.flow.Flow

interface PaycheckRepository {
    /** Paychecks for the signed-in user, soonest pay date first. */
    fun getPaychecks(): Flow<List<Paycheck>>

    /** Recurring payroll schedules for the signed-in user. */
    fun getPaycheckSchedules(): Flow<List<PaycheckSchedule>>

    suspend fun getPaycheck(id: Long): Paycheck?

    suspend fun addPaycheck(paycheck: Paycheck): Long

    suspend fun updatePaycheck(paycheck: Paycheck)

    suspend fun deletePaycheck(id: Long)

    /**
     * Saves a recurring payroll rule and materializes its upcoming occurrences.
     *
     * [billIds] are assigned to the first generated cutoff, which is the
     * paycheck the user configured in the form. Later cutoffs start with no
     * bills so every bill is covered by only one paycheck.
     */
    suspend fun addPaycheckSchedule(
        schedule: PaycheckSchedule,
        billIds: List<Long> = emptyList()
    ): Long

    /** Stops future generation while keeping the paychecks already created. */
    suspend fun stopPaycheckSchedule(scheduleClientId: String)

    /**
     * Uploads schedules, paychecks, their bill assignments, and queued
     * deletions so the plan can be restored on another device.
     */
    suspend fun syncPaychecks(userId: String)
}
