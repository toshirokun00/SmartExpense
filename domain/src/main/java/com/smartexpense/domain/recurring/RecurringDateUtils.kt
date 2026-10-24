package com.smartexpense.domain.recurring

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Converts between the app's epoch-millisecond date representation and
 * calendar dates.
 *
 * Recurring occurrences are stored at the start of the day in [zoneId] so that
 * date comparisons are stable regardless of the time of day an expense was
 * created or processed.
 */
object RecurringDateUtils {

    fun toLocalDate(
        epochMillis: Long,
        zoneId: ZoneId
    ): LocalDate {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(zoneId)
            .toLocalDate()
    }

    fun toEpochMillis(
        date: LocalDate,
        zoneId: ZoneId
    ): Long {
        return date
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    fun endOfDayMillis(
        date: LocalDate,
        zoneId: ZoneId
    ): Long {
        return date
            .plusDays(1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli() - 1L
    }
}
