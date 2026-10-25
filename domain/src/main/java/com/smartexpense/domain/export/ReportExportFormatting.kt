package com.smartexpense.domain.export

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId

/**
 * Shared display formatting for report exports.
 *
 * Amounts use plain decimal strings so CSV cells remain machine-readable and
 * PDF output matches the same values. Dates use ISO 8601 in the export
 * timezone.
 */
object ReportExportFormatting {
    const val CURRENCY_CODE = "PHP"
    const val UNKNOWN_CATEGORY = "Unknown Category"

    private const val MONEY_SCALE = 2
    private const val PERCENTAGE_SCALE = 2

    fun formatAmount(amount: Double): String {
        return BigDecimal.valueOf(amount)
            .setScale(MONEY_SCALE, RoundingMode.HALF_UP)
            .toPlainString()
    }

    fun formatPercentage(percentage: Double): String {
        return BigDecimal.valueOf(percentage)
            .setScale(PERCENTAGE_SCALE, RoundingMode.HALF_UP)
            .toPlainString()
    }

    fun formatDate(
        epochMillis: Long,
        zoneId: ZoneId
    ): String {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(zoneId)
            .toLocalDate()
            .toString()
    }

    fun formatInclusiveEndDate(
        endDateExclusive: Long,
        zoneId: ZoneId
    ): String {
        return formatDate(endDateExclusive - 1L, zoneId)
    }

    fun fileName(
        prefix: String,
        startDateInclusive: Long,
        endDateExclusive: Long,
        format: ReportExportFormat,
        zoneId: ZoneId
    ): String {
        val startDate = formatDate(startDateInclusive, zoneId)
        val endDate = formatInclusiveEndDate(endDateExclusive, zoneId)

        return "${prefix}_${startDate}_${endDate}.${format.fileExtension}"
    }
}
