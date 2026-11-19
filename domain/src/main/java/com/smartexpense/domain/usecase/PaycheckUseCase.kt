package com.smartexpense.domain.usecase

import com.smartexpense.domain.model.Paycheck
import com.smartexpense.domain.model.PaycheckSchedule
import com.smartexpense.domain.repository.PaycheckRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class GetPaychecksUseCase(private val repo: PaycheckRepository) {
    operator fun invoke(): Flow<List<Paycheck>> = repo.getPaychecks()
}

class GetPaycheckSchedulesUseCase(private val repo: PaycheckRepository) {
    operator fun invoke(): Flow<List<PaycheckSchedule>> = repo.getPaycheckSchedules()
}

class GetPaycheckUseCase(private val repo: PaycheckRepository) {
    suspend operator fun invoke(id: Long): Paycheck? = repo.getPaycheck(id)
}

class AddPaycheckUseCase(private val repo: PaycheckRepository) {
    suspend operator fun invoke(paycheck: Paycheck): Long = repo.addPaycheck(paycheck)
}

class UpdatePaycheckUseCase(private val repo: PaycheckRepository) {
    suspend operator fun invoke(paycheck: Paycheck) = repo.updatePaycheck(paycheck)
}

class DeletePaycheckUseCase(private val repo: PaycheckRepository) {
    suspend operator fun invoke(id: Long) = repo.deletePaycheck(id)
}

class AddPaycheckScheduleUseCase(private val repo: PaycheckRepository) {
    suspend operator fun invoke(
        schedule: PaycheckSchedule,
        billIds: List<Long> = emptyList()
    ): Long = repo.addPaycheckSchedule(schedule, billIds)
}

class StopPaycheckScheduleUseCase(private val repo: PaycheckRepository) {
    /**
     * Stops the schedule, keeps the paycheck at [fromPayDateMillis], and
     * removes every later cutoff, so no future income is planned after the
     * month the user stops repeating.
     */
    suspend operator fun invoke(
        scheduleClientId: String,
        fromPayDateMillis: Long
    ) = repo.stopPaycheckSchedule(scheduleClientId, fromPayDateMillis)
}

/**
 * Materializes the recurring paychecks that fall inside a month the user is
 * browsing, so every month of an active payroll schedule has its cutoff.
 */
class EnsurePaycheckOccurrencesUseCase(private val repo: PaycheckRepository) {
    suspend operator fun invoke(from: LocalDate, to: LocalDate) =
        repo.ensurePaycheckOccurrences(from, to)
}
