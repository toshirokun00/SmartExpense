package com.smartexpense.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * How often a [Bill] repeats. [NONE] means a one-time bill.
 */
enum class BillFrequency {
    NONE,
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    YEARLY
}

/**
 * Whether the bill amount is fixed ([STATIC]) or varies with usage ([VARIABLE]),
 * e.g. electricity or water bills.
 */
enum class BillAmountType {
    STATIC,
    VARIABLE
}

/**
 * Computed state of a bill occurrence. Never persisted: it is derived from the
 * due date, [Bill.paidAtMillis], [Bill.skipped] and [Bill.isActive].
 */
enum class BillStatus {
    UPCOMING,
    DUE_SOON,
    DUE_TODAY,
    OVERDUE,
    PAID,
    SKIPPED,
    PAUSED;

    fun label(): String = when (this) {
        UPCOMING -> "Upcoming"
        DUE_SOON -> "Due soon"
        DUE_TODAY -> "Due today"
        OVERDUE -> "Overdue"
        PAID -> "Paid"
        SKIPPED -> "Skipped"
        PAUSED -> "Paused"
    }
}

data class Bill(
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val dueDateMillis: Long,
    val notes: String = "",
    val frequency: BillFrequency = BillFrequency.NONE,
    /**
     * Number of frequency cycles between occurrences, e.g. `2` with
     * [BillFrequency.WEEKLY] means every two weeks.
     */
    val repeatInterval: Int = 1,
    val amountType: BillAmountType = BillAmountType.STATIC,
    /**
     * True while [amount] is the estimate for a variable bill rather than an
     * amount the user has confirmed by paying.
     */
    val amountEstimated: Boolean = false,
    val categoryId: Long? = null,
    val payee: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val autopay: Boolean = false,
    val reminderDaysBefore: Int = 3,
    /** Minutes since midnight for the reminder, e.g. 540 = 09:00. */
    val reminderTimeMinutes: Int = 9 * 60,
    val paidAtMillis: Long? = null,
    val skipped: Boolean = false,
    val isActive: Boolean = true
) {

    /** Whether this bill repeats on a schedule. */
    val isRecurring: Boolean
        get() = frequency != BillFrequency.NONE

    /** A bill counts as paid once a paid timestamp has been recorded. */
    val isPaid: Boolean
        get() = paidAtMillis != null

    /** Human readable label for the frequency, e.g. "Monthly". */
    fun frequencyLabel(): String = when (frequency) {
        BillFrequency.NONE -> "One-time"
        BillFrequency.WEEKLY -> "Weekly"
        BillFrequency.MONTHLY -> "Monthly"
        BillFrequency.QUARTERLY -> "Quarterly"
        BillFrequency.YEARLY -> "Yearly"
    }

    /** Label including the repeat interval, e.g. "Every 2 weeks". */
    fun repeatLabel(): String {
        if (!isRecurring) return "One-time"
        val interval = repeatInterval.coerceAtLeast(1)
        if (interval == 1) return frequencyLabel()
        val unit = when (frequency) {
            BillFrequency.WEEKLY -> "weeks"
            BillFrequency.MONTHLY -> "months"
            BillFrequency.QUARTERLY -> "quarters"
            BillFrequency.YEARLY -> "years"
            BillFrequency.NONE -> ""
        }
        return "Every $interval $unit"
    }

    fun dueDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(dueDateMillis).atZone(zoneId).toLocalDate()

    /**
     * Computes the visual status for this occurrence. Status is never stored;
     * it always reflects the current date and the persisted paid/skipped flags.
     */
    fun status(
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): BillStatus {
        if (!isActive) return BillStatus.PAUSED
        if (skipped) return BillStatus.SKIPPED
        if (isPaid) return BillStatus.PAID

        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val due = dueDate(zoneId)
        val daysUntilDue = ChronoUnit.DAYS.between(today, due)

        return when {
            daysUntilDue < 0 -> BillStatus.OVERDUE
            daysUntilDue == 0L -> BillStatus.DUE_TODAY
            daysUntilDue <= reminderDaysBefore.coerceAtLeast(0) -> BillStatus.DUE_SOON
            else -> BillStatus.UPCOMING
        }
    }

    /**
     * True when a variable bill is close to its due date but the amount is
     * still only an estimate, so the user should enter the actual amount.
     */
    fun needsAmount(
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Boolean {
        if (amountType != BillAmountType.VARIABLE || !amountEstimated) return false
        return when (status(nowMillis, zoneId)) {
            BillStatus.DUE_SOON,
            BillStatus.DUE_TODAY,
            BillStatus.OVERDUE -> true
            else -> false
        }
    }

    /**
     * Returns the next due date after [dueDateMillis] for a recurring bill, or
     * `null` for a one-time bill. Dates are advanced with calendar arithmetic
     * (never fixed millisecond math) so month lengths are handled correctly.
     */
    fun nextDueDateMillis(zoneId: ZoneId = ZoneId.systemDefault()): Long? {
        if (frequency == BillFrequency.NONE) return null

        val current = dueDate(zoneId)
        val step = repeatInterval.coerceAtLeast(1)

        val next = when (frequency) {
            BillFrequency.WEEKLY -> current.plusWeeks(step.toLong())
            BillFrequency.MONTHLY -> current.plusMonths(step.toLong())
            BillFrequency.QUARTERLY -> current.plusMonths(step * 3L)
            BillFrequency.YEARLY -> current.plusYears(step.toLong())
            BillFrequency.NONE -> return null
        }

        return next.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }
}
