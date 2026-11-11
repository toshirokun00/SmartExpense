package com.smartexpense.domain

import com.smartexpense.domain.model.Bill
import com.smartexpense.domain.model.BillAmountType
import com.smartexpense.domain.model.BillFrequency
import com.smartexpense.domain.model.BillStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class BillTest {

    private val zone = ZoneId.of("UTC")

    private fun millis(year: Int, month: Int, day: Int): Long {
        return LocalDate.of(year, month, day)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
    }

    private fun bill(
        dueDate: Long,
        frequency: BillFrequency
    ) = Bill(
        name = "Rent",
        amount = 1200.0,
        dueDateMillis = dueDate,
        frequency = frequency,
        amountType = BillAmountType.STATIC
    )

    @Test
    fun oneTimeBillHasNoNextDueDate() {
        val bill = bill(millis(2026, 1, 15), BillFrequency.NONE)
        assertNull(bill.nextDueDateMillis(zone))
        assertFalse(bill.isRecurring)
        assertEquals("One-time", bill.frequencyLabel())
    }

    @Test
    fun weeklyBillAdvancesSevenDays() {
        val bill = bill(millis(2026, 1, 15), BillFrequency.WEEKLY)
        assertEquals(
            millis(2026, 1, 22),
            bill.nextDueDateMillis(zone)
        )
        assertTrue(bill.isRecurring)
    }

    @Test
    fun monthlyBillAdvancesOneMonth() {
        val bill = bill(millis(2026, 1, 15), BillFrequency.MONTHLY)
        assertEquals(
            millis(2026, 2, 15),
            bill.nextDueDateMillis(zone)
        )
        assertEquals("Monthly", bill.frequencyLabel())
    }

    @Test
    fun quarterlyBillAdvancesThreeMonths() {
        val bill = bill(millis(2026, 1, 15), BillFrequency.QUARTERLY)
        assertEquals(
            millis(2026, 4, 15),
            bill.nextDueDateMillis(zone)
        )
        assertEquals("Quarterly", bill.frequencyLabel())
    }

    @Test
    fun yearlyBillAdvancesOneYear() {
        val bill = bill(millis(2026, 1, 15), BillFrequency.YEARLY)
        assertEquals(
            millis(2027, 1, 15),
            bill.nextDueDateMillis(zone)
        )
        assertEquals("Yearly", bill.frequencyLabel())
    }

    @Test
    fun monthlyBillClampsToShorterMonth() {
        val bill = bill(millis(2026, 1, 31), BillFrequency.MONTHLY)
        assertEquals(
            millis(2026, 2, 28),
            bill.nextDueDateMillis(zone)
        )
    }

    @Test
    fun nextDueDateUsesCalendarArithmeticAcrossDst() {
        val dstZone = ZoneId.of("America/New_York")
        val start = LocalDate.of(2026, 3, 1)
            .atStartOfDay(dstZone)
            .toInstant()
            .toEpochMilli()

        val next = bill(start, BillFrequency.MONTHLY)
            .nextDueDateMillis(dstZone)

        val nextDate = Instant.ofEpochMilli(next!!)
            .atZone(dstZone)
            .toLocalDate()

        assertEquals(LocalDate.of(2026, 4, 1), nextDate)
    }

    @Test
    fun repeatIntervalAdvancesMultipleCycles() {
        val bill = bill(millis(2026, 1, 15), BillFrequency.WEEKLY)
            .copy(repeatInterval = 3)

        assertEquals(
            millis(2026, 2, 5),
            bill.nextDueDateMillis(zone)
        )
        assertEquals("Every 3 weeks", bill.repeatLabel())
    }

    @Test
    fun statusIsComputedFromDueDate() {
        val today = millis(2026, 6, 10)
        val dueToday = bill(today, BillFrequency.NONE)
        val overdue = bill(millis(2026, 6, 1), BillFrequency.NONE)
        val upcoming = bill(millis(2026, 6, 30), BillFrequency.NONE)

        assertEquals(BillStatus.DUE_TODAY, dueToday.status(today, zone))
        assertEquals(BillStatus.OVERDUE, overdue.status(today, zone))
        assertEquals(BillStatus.UPCOMING, upcoming.status(today, zone))
    }

    @Test
    fun statusUsesReminderWindowForDueSoon() {
        val today = millis(2026, 6, 10)
        val bill = bill(millis(2026, 6, 12), BillFrequency.NONE)
            .copy(reminderDaysBefore = 3)

        assertEquals(BillStatus.DUE_SOON, bill.status(today, zone))
    }

    @Test
    fun paidSkippedAndPausedStatusesTakePriority() {
        val today = millis(2026, 6, 10)
        val base = bill(millis(2026, 6, 9), BillFrequency.MONTHLY)

        assertEquals(
            BillStatus.PAID,
            base.copy(paidAtMillis = today).status(today, zone)
        )
        assertEquals(
            BillStatus.SKIPPED,
            base.copy(skipped = true).status(today, zone)
        )
        assertEquals(
            BillStatus.PAUSED,
            base.copy(isActive = false).status(today, zone)
        )
    }

    @Test
    fun variableBillNeedsAmountOnlyWhenNearDue() {
        val today = millis(2026, 6, 10)
        val variable = Bill(
            name = "Meralco",
            amount = 2400.0,
            dueDateMillis = millis(2026, 6, 11),
            amountType = BillAmountType.VARIABLE,
            amountEstimated = true,
            reminderDaysBefore = 3
        )

        assertTrue(variable.needsAmount(today, zone))
        assertFalse(
            variable.copy(amountEstimated = false).needsAmount(today, zone)
        )
        assertFalse(
            variable.copy(dueDateMillis = millis(2026, 6, 30))
                .needsAmount(today, zone)
        )
    }
}
