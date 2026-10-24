package com.smartexpense.domain.recurring

import com.smartexpense.domain.model.RecurrenceFrequency
import com.smartexpense.domain.model.RecurringExpenseSchedule
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Calendar-based recurrence rules.
 *
 * Monthly schedules remember the day of month of the schedule's start date.
 * When a month is shorter than that day the occurrence is clamped to the last
 * valid day, and the original day is used again in later months. Yearly
 * schedules use the start date's month and day; February 29 falls back to
 * February 28 in non-leap years and returns to February 29 in leap years.
 *
 * All calculations use [LocalDate], never fixed millisecond arithmetic.
 */
object RecurrenceCalculator {

    /**
     * Safety limit for catch-up loops. This is far above any realistic number
     * of missed occurrences and prevents a corrupt schedule from looping
     * forever.
     */
    const val MAX_OCCURRENCES_PER_RUN = 10_000

    fun nextOccurrence(
        schedule: RecurringExpenseSchedule,
        current: LocalDate,
        zoneId: ZoneId
    ): LocalDate {
        val anchor = RecurringDateUtils.toLocalDate(
            epochMillis = schedule.startDate,
            zoneId = zoneId
        )

        return when (schedule.frequency) {
            RecurrenceFrequency.WEEKLY -> current.plusWeeks(
                schedule.interval.toLong()
            )

            RecurrenceFrequency.MONTHLY -> {
                val targetMonth = YearMonth
                    .from(current)
                    .plusMonths(schedule.interval.toLong())

                val day = minOf(
                    anchor.dayOfMonth,
                    targetMonth.lengthOfMonth()
                )

                targetMonth.atDay(day)
            }

            RecurrenceFrequency.YEARLY -> {
                val targetYear = current.year + schedule.interval

                val day = minOf(
                    anchor.dayOfMonth,
                    YearMonth.of(targetYear, anchor.month)
                        .lengthOfMonth()
                )

                LocalDate.of(
                    targetYear,
                    anchor.month,
                    day
                )
            }
        }
    }

    /**
     * Returns every occurrence that is due on or before [today] and within the
     * schedule's start/end bounds, starting at [RecurringExpenseSchedule.nextOccurrence].
     *
     * The returned list is ordered from oldest to newest and never contains a
     * date before the start date or after the end date.
     */
    fun dueOccurrences(
        schedule: RecurringExpenseSchedule,
        today: LocalDate,
        zoneId: ZoneId
    ): List<LocalDate> {
        val start = RecurringDateUtils.toLocalDate(
            epochMillis = schedule.startDate,
            zoneId = zoneId
        )

        val end = schedule.endDate?.let { endDate ->
            RecurringDateUtils.toLocalDate(endDate, zoneId)
        }

        var current = RecurringDateUtils.toLocalDate(
            epochMillis = schedule.nextOccurrence,
            zoneId = zoneId
        )

        if (current.isBefore(start)) {
            current = start
        }

        val occurrences = mutableListOf<LocalDate>()
        var guard = 0

        while (
            !current.isAfter(today) &&
            (end == null || !current.isAfter(end)) &&
            guard < MAX_OCCURRENCES_PER_RUN
        ) {
            if (!current.isBefore(start)) {
                occurrences += current
            }

            current = nextOccurrence(
                schedule = schedule,
                current = current,
                zoneId = zoneId
            )
            guard++
        }

        return occurrences
    }

    /**
     * Returns the first occurrence on or after [date].
     *
     * Used when resuming a paused schedule so occurrences that became due while
     * the schedule was paused are intentionally skipped. If the schedule has
     * already ended, the returned date is the first recurrence after the end
     * date.
     */
    fun firstOnOrAfter(
        schedule: RecurringExpenseSchedule,
        date: LocalDate,
        zoneId: ZoneId
    ): LocalDate {
        val start = RecurringDateUtils.toLocalDate(
            epochMillis = schedule.startDate,
            zoneId = zoneId
        )

        val end = schedule.endDate?.let { endDate ->
            RecurringDateUtils.toLocalDate(endDate, zoneId)
        }

        var current = RecurringDateUtils.toLocalDate(
            epochMillis = schedule.nextOccurrence,
            zoneId = zoneId
        )

        if (current.isBefore(start)) {
            current = start
        }

        var guard = 0
        while (
            current.isBefore(date) &&
            (end == null || !current.isAfter(end)) &&
            guard < MAX_OCCURRENCES_PER_RUN
        ) {
            current = nextOccurrence(
                schedule = schedule,
                current = current,
                zoneId = zoneId
            )
            guard++
        }

        return current
    }

    fun isWithinBounds(
        schedule: RecurringExpenseSchedule,
        date: LocalDate,
        zoneId: ZoneId
    ): Boolean {
        val start = RecurringDateUtils.toLocalDate(
            epochMillis = schedule.startDate,
            zoneId = zoneId
        )

        val end = schedule.endDate?.let { endDate ->
            RecurringDateUtils.toLocalDate(endDate, zoneId)
        }

        return !date.isBefore(start) &&
            (end == null || !date.isAfter(end))
    }
}
