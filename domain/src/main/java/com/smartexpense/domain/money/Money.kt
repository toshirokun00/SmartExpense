package com.smartexpense.domain.money

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Money is persisted as integer minor units (centavos) so repeated arithmetic
 * cannot accumulate floating-point error. Doubles are only used at the edges:
 * parsing user input, display formatting, and the existing report/budget
 * calculations that still operate on [Double].
 */
object Money {

    const val SCALE = 2

    fun toMinorUnits(amount: Double): Long =
        toMinorUnits(BigDecimal.valueOf(amount))

    fun toMinorUnits(amount: BigDecimal): Long =
        amount
            .setScale(SCALE, RoundingMode.HALF_UP)
            .movePointRight(SCALE)
            .longValueExact()

    fun fromMinorUnits(minorUnits: Long): BigDecimal =
        BigDecimal.valueOf(minorUnits, SCALE)

    fun toDouble(minorUnits: Long): Double =
        fromMinorUnits(minorUnits).toDouble()

    fun format(minorUnits: Long): String =
        fromMinorUnits(minorUnits)
            .setScale(SCALE, RoundingMode.UNNECESSARY)
            .toPlainString()
}
