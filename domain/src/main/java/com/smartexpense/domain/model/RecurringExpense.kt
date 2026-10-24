package com.smartexpense.domain.model

import java.util.UUID

/**
 * How often a recurring expense schedule produces an occurrence.
 */
enum class RecurrenceFrequency {
    WEEKLY,
    MONTHLY,
    YEARLY
}

/**
 * Lifecycle state of a recurring expense schedule as shown in the UI.
 *
 * A schedule is [PAUSED] while it is inactive, and [ENDED] once its next
 * occurrence would fall after its optional end date.
 */
enum class RecurringScheduleStatus {
    ACTIVE,
    PAUSED,
    ENDED
}

/**
 * A user-owned schedule that generates normal [Expense] rows on its due dates.
 *
 * Dates are stored as epoch milliseconds at the start of the day in the
 * configured time zone. [nextOccurrence] is the first occurrence that has not
 * been generated yet.
 *
 * The [clientId] is generated once and reused across retries so duplicate
 * submissions cannot create duplicate schedules. The occurrence expenses use a
 * deterministic client id derived from this value and the occurrence date.
 */
data class RecurringExpenseSchedule(
    val id: Long = 0L,
    val clientId: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val description: String,
    val amount: Double,
    val categoryId: Long,
    val frequency: RecurrenceFrequency,
    val interval: Int = 1,
    val startDate: Long,
    val endDate: Long? = null,
    val nextOccurrence: Long = startDate,
    val isActive: Boolean = true
) {

    fun status(): RecurringScheduleStatus {
        return when {
            !isActive -> RecurringScheduleStatus.PAUSED
            endDate != null && nextOccurrence > endDate ->
                RecurringScheduleStatus.ENDED

            else -> RecurringScheduleStatus.ACTIVE
        }
    }
}
