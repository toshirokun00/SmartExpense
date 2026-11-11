package com.smartexpense.bills.util

import com.smartexpense.core.common.util.MoneyInputUtil
import com.smartexpense.domain.model.BillFrequency
import java.util.Locale

internal val REMINDER_DAY_OPTIONS = listOf(0, 1, 3, 7)

internal fun reminderDaysLabel(days: Int): String = when (days) {
    0 -> "On the day"
    1 -> "1 day before"
    else -> "$days days before"
}

internal fun reminderDaysLabel(days: Set<Int>): String {
    if (days.isEmpty()) return "None"
    if (days.size == 1) return reminderDaysLabel(days.first())
    return days.sorted().map { reminderDaysLabel(it) }.joinToString(", ")
}

internal fun BillFrequency.intervalUnitLabel(): String = when (this) {
    BillFrequency.WEEKLY -> "week(s)"
    BillFrequency.MONTHLY -> "month(s)"
    BillFrequency.QUARTERLY -> "quarter(s)"
    BillFrequency.YEARLY -> "year(s)"
    BillFrequency.NONE -> ""
}

internal fun formatReminderTime(minutes: Int): String {
    val normalized = minutes.coerceIn(0, 23 * 60 + 59)
    val hour = normalized / 60
    val minute = normalized % 60
    val amPm = if (hour < 12) "AM" else "PM"
    val hour12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, amPm)
}

internal fun formatAmountForEditing(amount: Double): String {
    val raw = if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()
    return MoneyInputUtil.formatAmountInput(raw)
}
