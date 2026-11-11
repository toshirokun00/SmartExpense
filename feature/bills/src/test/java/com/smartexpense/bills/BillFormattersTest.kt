package com.smartexpense.bills

import com.smartexpense.bills.util.formatAmountForEditing
import com.smartexpense.bills.util.formatReminderTime
import com.smartexpense.bills.util.intervalUnitLabel
import com.smartexpense.bills.util.reminderDaysLabel
import com.smartexpense.domain.model.BillFrequency
import org.junit.Assert.assertEquals
import org.junit.Test

class BillFormattersTest {

    // --------------------------------------------------
    // Reminder day labels
    // --------------------------------------------------

    @Test
    fun reminderLabelOnTheDay() {
        assertEquals("On the day", reminderDaysLabel(0))
    }

    @Test
    fun reminderLabelOneDayBefore() {
        assertEquals("1 day before", reminderDaysLabel(1))
    }

    @Test
    fun reminderLabelMultipleDaysBefore() {
        assertEquals("7 days before", reminderDaysLabel(7))
    }

    @Test
    fun reminderSetLabelIsNoneWhenEmpty() {
        assertEquals("None", reminderDaysLabel(emptySet()))
    }

    @Test
    fun reminderSetLabelUsesSingleDayLabel() {
        assertEquals("On the day", reminderDaysLabel(setOf(0)))
    }

    @Test
    fun reminderSetLabelIsSortedAndJoined() {
        assertEquals(
            "On the day, 1 day before, 7 days before",
            reminderDaysLabel(setOf(7, 0, 1))
        )
    }

    // --------------------------------------------------
    // Frequency unit labels
    // --------------------------------------------------

    @Test
    fun intervalUnitLabels() {
        assertEquals("week(s)", BillFrequency.WEEKLY.intervalUnitLabel())
        assertEquals("month(s)", BillFrequency.MONTHLY.intervalUnitLabel())
        assertEquals("quarter(s)", BillFrequency.QUARTERLY.intervalUnitLabel())
        assertEquals("year(s)", BillFrequency.YEARLY.intervalUnitLabel())
        assertEquals("", BillFrequency.NONE.intervalUnitLabel())
    }

    // --------------------------------------------------
    // Reminder time
    // --------------------------------------------------

    @Test
    fun reminderTimeFormatsMorning() {
        assertEquals("9:00 AM", formatReminderTime(9 * 60))
    }

    @Test
    fun reminderTimeFormatsAfternoon() {
        assertEquals("1:30 PM", formatReminderTime(13 * 60 + 30))
    }

    @Test
    fun reminderTimeFormatsNoon() {
        assertEquals("12:00 PM", formatReminderTime(12 * 60))
    }

    @Test
    fun reminderTimeFormatsMidnight() {
        assertEquals("12:00 AM", formatReminderTime(0))
    }

    @Test
    fun reminderTimeIsClampedToTheDay() {
        assertEquals("11:59 PM", formatReminderTime(24 * 60 + 30))
        assertEquals("12:00 AM", formatReminderTime(-10))
    }

    // --------------------------------------------------
    // Amount editing
    // --------------------------------------------------

    @Test
    fun wholeAmountHasNoDecimalsOrCommasBelowThousand() {
        assertEquals("900", formatAmountForEditing(900.0))
    }

    @Test
    fun amountIsGroupedWithCommas() {
        assertEquals("4,750", formatAmountForEditing(4750.0))
    }

    @Test
    fun fractionalAmountKeepsDecimals() {
        assertEquals("1,234.5", formatAmountForEditing(1234.5))
    }
}
