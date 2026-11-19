package com.smartexpense.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * How often a payroll schedule pays out.
 *
 * [SEMI_MONTHLY] covers the common Philippine cut-off pattern where the user is
 * paid twice a month, for example every 15th and 30th.
 */
enum class PaycheckFrequency {
    WEEKLY,
    BIWEEKLY,
    MONTHLY,
    SEMI_MONTHLY;

    fun label(): String = when (this) {
        WEEKLY -> "Weekly"
        BIWEEKLY -> "Every 2 weeks"
        MONTHLY -> "Monthly"
        SEMI_MONTHLY -> "Semi-monthly"
    }
}

/**
 * A recurring payroll rule. Each occurrence is materialized as a normal
 * [Paycheck] so the user can edit one cutoff without changing the others.
 *
 * [firstDay] and [secondDay] are day-of-month anchors for monthly schedules.
 * Short months clamp to their last day (the 30th becomes February 28/29) and
 * the original anchor returns in the next month.
 */
data class PaycheckSchedule(
    val id: Long = 0,
    val clientId: String,
    val amount: Double,
    val label: String = "",
    val frequency: PaycheckFrequency = PaycheckFrequency.SEMI_MONTHLY,
    val firstDay: Int = 15,
    val secondDay: Int? = 30,
    val startDateMillis: Long,
    val endDateMillis: Long? = null,
    val isActive: Boolean = true,
    val createdAt: Long = 0
) {
    fun displayLabel(): String = label.ifBlank { "Paycheck" }

    fun startDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(startDateMillis).atZone(zoneId).toLocalDate()

    fun endDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate? =
        endDateMillis?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() }

    /** Day-of-month anchors, normalized and de-duplicated for this frequency. */
    fun payDays(): List<Int> = when (frequency) {
        PaycheckFrequency.MONTHLY -> listOf(firstDay.coerceIn(1, 31))

        PaycheckFrequency.SEMI_MONTHLY -> listOf(firstDay, secondDay ?: 30)
            .map { it.coerceIn(1, 31) }
            .distinct()
            .sorted()

        PaycheckFrequency.WEEKLY,
        PaycheckFrequency.BIWEEKLY -> emptyList()
    }

    /** Human readable rule, e.g. "Semi-monthly · 15th & 30th". */
    fun repeatLabel(): String = when (frequency) {
        PaycheckFrequency.WEEKLY -> "Weekly"
        PaycheckFrequency.BIWEEKLY -> "Every 2 weeks"
        PaycheckFrequency.MONTHLY -> "Monthly · ${ordinalDay(firstDay)}"
        PaycheckFrequency.SEMI_MONTHLY -> {
            val days = payDays()
            "Semi-monthly · ${days.joinToString(" & ") { ordinalDay(it) }}"
        }
    }

    /**
     * Every occurrence inside [from]..[to], also bounded by the schedule's own
     * start and end dates. Ordered oldest first.
     */
    fun occurrencesBetween(
        from: LocalDate,
        to: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<LocalDate> {
        val start = startDate(zoneId)
        val end = endDate(zoneId)
        val lower = maxOf(from, start)
        val upper = minOf(to, end ?: to)
        if (lower.isAfter(upper)) return emptyList()

        return when (frequency) {
            PaycheckFrequency.MONTHLY,
            PaycheckFrequency.SEMI_MONTHLY -> monthlyOccurrences(lower, upper)

            PaycheckFrequency.WEEKLY,
            PaycheckFrequency.BIWEEKLY -> intervalOccurrences(
                start = start,
                lower = lower,
                upper = upper,
                periodDays = if (frequency == PaycheckFrequency.WEEKLY) 7L else 14L
            )
        }
    }

    private fun monthlyOccurrences(
        lower: LocalDate,
        upper: LocalDate
    ): List<LocalDate> {
        val days = payDays()
        if (days.isEmpty()) return emptyList()

        val occurrences = mutableListOf<LocalDate>()
        var month = YearMonth.from(lower)
        val lastMonth = YearMonth.from(upper)

        while (!month.isAfter(lastMonth)) {
            for (day in days) {
                val date = month.atDay(minOf(day, month.lengthOfMonth()))
                if (!date.isBefore(lower) && !date.isAfter(upper)) {
                    occurrences += date
                }
            }
            month = month.plusMonths(1)
        }

        return occurrences.distinct().sorted()
    }

    private fun intervalOccurrences(
        start: LocalDate,
        lower: LocalDate,
        upper: LocalDate,
        periodDays: Long
    ): List<LocalDate> {
        val daysSinceStart = ChronoUnit.DAYS.between(start, lower)
        val steps = if (daysSinceStart <= 0L) {
            0L
        } else {
            (daysSinceStart + periodDays - 1) / periodDays
        }

        val occurrences = mutableListOf<LocalDate>()
        var date = start.plusDays(steps * periodDays)
        while (!date.isAfter(upper)) {
            occurrences += date
            date = date.plusDays(periodDays)
        }
        return occurrences
    }
}

/** 1 -> "1st", 15 -> "15th", 30 -> "30th". */
fun ordinalDay(day: Int): String {
    val suffix = when {
        day % 100 in 11..13 -> "th"
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }
    return "$day$suffix"
}

/**
 * Deterministic client id for one occurrence. Keeping it stable means a retry
 * cannot create a duplicate paycheck locally or remotely.
 */
fun paycheckOccurrenceClientId(
    scheduleClientId: String,
    date: LocalDate
): String = UUID.nameUUIDFromBytes(
    "smart-expense-paycheck:$scheduleClientId:$date".toByteArray()
).toString()
