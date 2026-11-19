package com.smartexpense.domain

import com.smartexpense.domain.model.PaycheckFrequency
import com.smartexpense.domain.model.PaycheckSchedule
import com.smartexpense.domain.model.ordinalDay
import com.smartexpense.domain.model.paycheckOccurrenceClientId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class PaycheckScheduleTest {

    private val zone = ZoneOffset.UTC

    private fun schedule(
        frequency: PaycheckFrequency = PaycheckFrequency.SEMI_MONTHLY,
        firstDay: Int = 15,
        secondDay: Int? = 30,
        start: LocalDate = LocalDate.of(2026, 11, 15),
        end: LocalDate? = null
    ) = PaycheckSchedule(
        clientId = "schedule-1",
        amount = 2_000.0,
        frequency = frequency,
        firstDay = firstDay,
        secondDay = secondDay,
        startDateMillis = start.atStartOfDay(zone).toInstant().toEpochMilli(),
        endDateMillis = end?.let {
            it.atStartOfDay(zone).toInstant().toEpochMilli()
        }
    )

    @Test
    fun `semi-monthly schedules pay on both cutoffs`() {
        val dates = schedule().occurrencesBetween(
            from = LocalDate.of(2026, 11, 1),
            to = LocalDate.of(2026, 12, 31),
            zoneId = zone
        )

        assertEquals(
            listOf(
                LocalDate.of(2026, 11, 15),
                LocalDate.of(2026, 11, 30),
                LocalDate.of(2026, 12, 15),
                LocalDate.of(2026, 12, 30)
            ),
            dates
        )
    }

    @Test
    fun `short months clamp the 30th then return to it`() {
        val dates = schedule().occurrencesBetween(
            from = LocalDate.of(2027, 1, 1),
            to = LocalDate.of(2027, 4, 1),
            zoneId = zone
        )

        assertEquals(
            listOf(
                LocalDate.of(2027, 1, 15),
                LocalDate.of(2027, 1, 30),
                LocalDate.of(2027, 2, 15),
                LocalDate.of(2027, 2, 28),
                LocalDate.of(2027, 3, 15),
                LocalDate.of(2027, 3, 30)
            ),
            dates
        )
    }

    @Test
    fun `monthly schedules use one day and clamp it`() {
        val dates = schedule(
            frequency = PaycheckFrequency.MONTHLY,
            firstDay = 31,
            secondDay = null,
            start = LocalDate.of(2027, 1, 31)
        ).occurrencesBetween(
            from = LocalDate.of(2027, 1, 1),
            to = LocalDate.of(2027, 3, 31),
            zoneId = zone
        )

        assertEquals(
            listOf(
                LocalDate.of(2027, 1, 31),
                LocalDate.of(2027, 2, 28),
                LocalDate.of(2027, 3, 31)
            ),
            dates
        )
    }

    @Test
    fun `weekly and biweekly schedules step from the start date`() {
        val start = LocalDate.of(2026, 11, 10)
        val weekly = schedule(
            frequency = PaycheckFrequency.WEEKLY,
            secondDay = null,
            start = start
        ).occurrencesBetween(
            from = LocalDate.of(2026, 11, 1),
            to = LocalDate.of(2026, 12, 1),
            zoneId = zone
        )
        val biweekly = schedule(
            frequency = PaycheckFrequency.BIWEEKLY,
            secondDay = null,
            start = start
        ).occurrencesBetween(
            from = LocalDate.of(2026, 11, 1),
            to = LocalDate.of(2026, 12, 1),
            zoneId = zone
        )

        assertEquals(
            listOf(
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 17),
                LocalDate.of(2026, 11, 24),
                LocalDate.of(2026, 12, 1)
            ),
            weekly
        )
        assertEquals(
            listOf(
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 24)
            ),
            biweekly
        )
    }

    @Test
    fun `occurrences stop at the schedule end date`() {
        val dates = schedule(end = LocalDate.of(2026, 12, 15)).occurrencesBetween(
            from = LocalDate.of(2026, 11, 1),
            to = LocalDate.of(2027, 1, 31),
            zoneId = zone
        )

        assertEquals(
            listOf(
                LocalDate.of(2026, 11, 15),
                LocalDate.of(2026, 11, 30),
                LocalDate.of(2026, 12, 15)
            ),
            dates
        )
    }

    @Test
    fun `duplicate pay days are collapsed`() {
        assertEquals(
            listOf(15),
            schedule(firstDay = 15, secondDay = 15).payDays()
        )
    }

    @Test
    fun `occurrence client ids are deterministic and unique per date`() {
        val date = LocalDate.of(2026, 11, 15)
        val first = paycheckOccurrenceClientId("schedule-1", date)
        val retry = paycheckOccurrenceClientId("schedule-1", date)
        val nextCutoff = paycheckOccurrenceClientId("schedule-1", date.plusDays(15))
        val otherSchedule = paycheckOccurrenceClientId("schedule-2", date)

        assertEquals(first, retry)
        assertNotEquals(first, nextCutoff)
        assertNotEquals(first, otherSchedule)
    }

    @Test
    fun `repeat labels describe the rule`() {
        assertEquals("Semi-monthly · 15th & 30th", schedule().repeatLabel())
        assertEquals(
            "Monthly · 25th",
            schedule(
                frequency = PaycheckFrequency.MONTHLY,
                firstDay = 25,
                secondDay = null
            ).repeatLabel()
        )
        assertEquals("Weekly", schedule(frequency = PaycheckFrequency.WEEKLY).repeatLabel())
        assertEquals(
            "Every 2 weeks",
            schedule(frequency = PaycheckFrequency.BIWEEKLY).repeatLabel()
        )
    }

    @Test
    fun `ordinal day suffixes handle teens`() {
        assertEquals("1st", ordinalDay(1))
        assertEquals("2nd", ordinalDay(2))
        assertEquals("3rd", ordinalDay(3))
        assertEquals("11th", ordinalDay(11))
        assertEquals("12th", ordinalDay(12))
        assertEquals("21st", ordinalDay(21))
        assertEquals("30th", ordinalDay(30))
    }
}
