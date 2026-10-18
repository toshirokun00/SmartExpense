package com.smartexpense.core.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Extension functions for formatting dates consistently across the application.
 *
 * All dates are formatted using the device's default locale with the pattern "MMM dd, yyyy"
 * (e.g., "Oct 17, 2026"), matching the convention used throughout SmartExpense.
 */
fun Long.toDateString(): String {
    return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(this))
}

fun Date.toDateString(): String {
    return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(this)
}

/**
 * Formats a Long timestamp (milliseconds since epoch) to a date string.
 *
 * @param timestamp milliseconds since epoch
 * @return formatted date string, e.g. "Oct 17, 2026"
 */
fun formatDate(timestamp: Long): String = timestamp.toDateString()
