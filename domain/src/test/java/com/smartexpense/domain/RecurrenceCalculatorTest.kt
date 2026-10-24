package com.smartexpense.domain

import com.smartexpense.domain.model.RecurrenceFrequency
import com.smartexpense.domain.model.RecurringExpenseSchedule
import com.smartexpense.domain.recurring.RecurrenceCalculator
import com.smartexpense.domain.recurring.RecurringDateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class RecurrenceCalculatorTest {

    private val zone = ZoneId.of("UTC")

    @Test
    fun `weekly recurrence advances seven days`() {
        val schedule = schedule(
            start = "2026-01-05",
            frequency = RecurrenceFrequency.WEEKLY
        )

        val next = RecurrenceCalculator.nextOccurrence(
            schedule = schedule,
            current = date("2026-01-05"),
            zoneId = zone
        )

        assertEquals(date("2026-01-12"), next)
    }

    @Test
    fun `weekly recurrence respects the configured interval`() {
        val schedule = schedule(
            start = "2026-01-05",
            frequency = RecurrenceFrequency.WEEKLY,
            interval = 3
        )

        val next = RecurrenceCalculator.nextOccurrence(
            schedule = schedule,
            current = date("2026-01-05"),
            zoneId = zone
        )

        assertEquals(date("2026-01-26"), next)
    }

    @Test
    fun `monthly schedule clamps to the last day and restores the anchor day`() {
        val schedule = schedule(
            start = "2026-01-31",
            frequency = RecurrenceFrequency.MONTHLY
        )

        val february = RecurrenceCalculator.nextOccurrence(
            schedule = schedule,
            current = date("2026-01-31"),
            zoneId = zone
        )

        val march = RecurrenceCalculator.nextOccurrence(
            schedule = schedule,
            current = february,
            zoneId = zone
        )

        val april = RecurrenceCalculator.nextOccurrence(
            schedule = schedule,
            current = march,
            zoneId = zone
        )

        assertEquals(date("2026-02-28"), february)
        assertEquals(date("2026-03-31"), march)
        assertEquals(date("2026-04-30"), april)
    }

    @Test
    fun `monthly schedule keeps a thirtieth anchor across February`() {
        val schedule = schedule(
            start = "2024-01-30",
            frequency = RecurrenceFrequency.MONTHLY
        )

        val february = RecurrenceCalculator.nextOccurrence(
            schedule = schedule,
            current = date("2024-01-30"),
            zoneId = zone
        )

        val march = RecurrenceCalculator.nextOccurrence(
            schedule = schedule,
            current = february,
            zoneId = zone
        )

        assertEquals(date("2024-02-29"), february)
        assertEquals(date("2024-03-30"), march)
    }

    @Test
    fun `monthly schedule preserves a leap day anchor`() {
        val schedule = schedule(
            start = "2024-02-29",
            frequency = RecurrenceFrequency.MONTHLY
        )

        val march = RecurrenceCalculator.nextOccurrence(
            schedule = schedule,
            current = date("2024-02-29"),
            zoneId = zone
        )

        val april = RecurrenceCalculator.nextOccurrence(
            schedule = schedule,
            current = march,
            zoneId = zone
        )

        assertEquals(date("2024-03-29"), march)
        assertEquals(date("2024-04-29"), april)
    }

    @Test
    fun `monthly interval counts calendar months from the current occurrence`() {
        val schedule = schedule(
            start = "2026-01-31",
            frequency = RecurrenceFrequency.MONTHLY,
            interval = 2
        )

        val next = RecurrenceCalculator.nextOccurrence(
            schedule = schedule,
            current = date("2026-01-31"),
            zoneId = zone
        )

        assertEquals(date("2026-03-31"), next)
    }

    @Test
    fun `yearly leap day falls back to February 28 and returns on leap years`() {
        val schedule = schedule(
            start = "2024-02-29",
            frequency = RecurrenceFrequency.YEARLY
        )

        val year2025 = RecurrenceCalculator.nextOccurrence(
            schedule = schedule,
            current = date("2024-02-29"),
            zoneId = zone
        )

        val year2026 = RecurrenceCalculator.nextOccurrence(
            schedule = schedule,
            current = year2025,
            zoneId = zone
        )

        val year2027 = RecurrenceCalculator.nextOccurrence(
            schedule = schedule,
            current = year2026,
            zoneId = zone
        )

        val year2028 = RecurrenceCalculator.nextOccurrence(
            schedule = schedule,
            current = year2027,
            zoneId = zone
        )

        assertEquals(date("2025-02-28"), year2025)
        assertEquals(date("2026-02-28"), year2026)
        assertEquals(date("2027-02-28"), year2027)
        assertEquals(date("2028-02-29"), year2028)
    }

    @Test
    fun `yearly recurrence respects the configured interval`() {
        val schedule = schedule(
            start = "2026-06-15",
            frequency = RecurrenceFrequency.YEARLY,
            interval = 3
        )

        val next = RecurrenceCalculator.nextOccurrence(
            schedule = schedule,
            current = date("2026-06-15"),
            zoneId = zone
        )

        assertEquals(date("2029-06-15"), next)
    }

    @Test
    fun `due occurrences catch up every missed active occurrence`() {
        val schedule = schedule(
            start = "2026-01-05",
            frequency = RecurrenceFrequency.WEEKLY
        )

        val due = RecurrenceCalculator.dueOccurrences(
            schedule = schedule,
            today = date("2026-02-02"),
            zoneId = zone
        )

        assertEquals(
            listOf(
                date("2026-01-05"),
                date("2026-01-12"),
                date("2026-01-19"),
                date("2026-01-26"),
                date("2026-02-02")
            ),
            due
        )
    }

    @Test
    fun `due occurrences never start before the schedule start date`() {
        val schedule = schedule(
            start = "2026-03-01",
            frequency = RecurrenceFrequency.MONTHLY,
            next = "2026-01-01"
        )

        val due = RecurrenceCalculator.dueOccurrences(
            schedule = schedule,
            today = date("2026-04-01"),
            zoneId = zone
        )

        assertEquals(
            listOf(
                date("2026-03-01"),
                date("2026-04-01")
            ),
            due
        )
    }

    @Test
    fun `due occurrences stop at the end date`() {
        val schedule = schedule(
            start = "2026-01-05",
            frequency = RecurrenceFrequency.WEEKLY,
            end = "2026-01-19"
        )

        val due = RecurrenceCalculator.dueOccurrences(
            schedule = schedule,
            today = date("2026-03-01"),
            zoneId = zone
        )

        assertEquals(
            listOf(
                date("2026-01-05"),
                date("2026-01-12"),
                date("2026-01-19")
            ),
            due
        )
    }

    @Test
    fun `due occurrences respect the existing next occurrence`() {
        val schedule = schedule(
            start = "2026-01-05",
            frequency = RecurrenceFrequency.WEEKLY,
            next = "2026-01-19"
        )

        val due = RecurrenceCalculator.dueOccurrences(
            schedule = schedule,
            today = date("2026-01-26"),
            zoneId = zone
        )

        assertEquals(
            listOf(
                date("2026-01-19"),
                date("2026-01-26")
            ),
            due
        )
    }

    @Test
    fun `first occurrence on or after skips paused-period occurrences`() {
        val schedule = schedule(
            start = "2026-01-05",
            frequency = RecurrenceFrequency.WEEKLY,
            next = "2026-01-26"
        )

        val resumed = RecurrenceCalculator.firstOnOrAfter(
            schedule = schedule,
            date = date("2026-02-10"),
            zoneId = zone
        )

        assertEquals(date("2026-02-16"), resumed)
    }

    @Test
    fun `first occurrence on or after keeps a future occurrence`() {
        val schedule = schedule(
            start = "2026-01-05",
            frequency = RecurrenceFrequency.WEEKLY,
            next = "2026-03-02"
        )

        val resumed = RecurrenceCalculator.firstOnOrAfter(
            schedule = schedule,
            date = date("2026-02-10"),
            zoneId = zone
        )

        assertEquals(date("2026-03-02"), resumed)
    }

    @Test
    fun `monthly end date includes the final valid occurrence`() {
        val schedule = schedule(
            start = "2026-01-31",
            frequency = RecurrenceFrequency.MONTHLY,
            end = "2026-03-31"
        )

        val due = RecurrenceCalculator.dueOccurrences(
            schedule = schedule,
            today = date("2026-12-31"),
            zoneId = zone
        )

        assertEquals(
            listOf(
                date("2026-01-31"),
                date("2026-02-28"),
                date("2026-03-31")
            ),
            due
        )
    }

    @Test
    fun `status is ended once the next occurrence passes the end date`() {
        val schedule = schedule(
            start = "2026-01-31",
            frequency = RecurrenceFrequency.MONTHLY,
            end = "2026-03-31",
            next = "2026-04-30"
        )

        assertTrue(
            schedule.status() ==
                com.smartexpense.domain.model.RecurringScheduleStatus.ENDED
        )
    }

    private fun schedule(
        start: String,
        frequency: RecurrenceFrequency,
        interval: Int = 1,
        end: String? = null,
        next: String = start
    ): RecurringExpenseSchedule {
        return RecurringExpenseSchedule(
            clientId = "schedule-client-id",
            userId = "user-1",
            description = "Rent",
            amount = 1000.0,
            categoryId = 1L,
            frequency = frequency,
            interval = interval,
            startDate = millis(start),
            endDate = end?.let(::millis),
            nextOccurrence = millis(next)
        )
    }

    private fun date(value: String): LocalDate {
        return LocalDate.parse(value)
    }

    private fun millis(value: String): Long {
        return RecurringDateUtils.toEpochMillis(
            date = date(value),
            zoneId = zone
        )
    }
}
