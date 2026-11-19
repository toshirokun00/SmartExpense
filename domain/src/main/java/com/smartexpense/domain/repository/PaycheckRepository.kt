package com.smartexpense.domain.repository

import com.smartexpense.domain.model.Paycheck
import com.smartexpense.domain.model.PaycheckSchedule
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

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

    /**
     * Stops future generation, keeps the cutoff at [fromPayDateMillis], and
     * removes every later paycheck of the schedule. Calling it again on an
     * already stopped schedule still trims any paychecks left over from an
     * older build or a server restore.
     */
    suspend fun stopPaycheckSchedule(
        scheduleClientId: String,
        fromPayDateMillis: Long
    )

    /**
     * Creates any missing paychecks the active schedules pay inside
     * [from]..[to]. Browsing a month calls this so a recurring payroll shows a
     * cutoff for every month, not only for the rolling window that was
     * materialized the last time the app synced.
     */
    suspend fun ensurePaycheckOccurrences(from: LocalDate, to: LocalDate)

    /**
     * Uploads schedules, paychecks, their bill assignments, and queued
     * deletions so the plan can be restored on another device.
     */
    suspend fun syncPaychecks(userId: String)
}
