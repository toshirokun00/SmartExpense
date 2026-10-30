package com.smartexpense.domain

import com.smartexpense.domain.money.Money
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {

    @Test
    fun convertsDoubleToMinorUnitsWithHalfUpRounding() {
        assertEquals(10050L, Money.toMinorUnits(100.50))
        assertEquals(1L, Money.toMinorUnits(0.005))
        assertEquals(0L, Money.toMinorUnits(0.004))
    }

    @Test
    fun convertsMinorUnitsBackToDecimal() {
        assertEquals(BigDecimal("100.50"), Money.fromMinorUnits(10050L))
        assertEquals("100.50", Money.format(10050L))
    }

    @Test
    fun roundTripThroughMinorUnitsIsStable() {
        val amounts = listOf(0.01, 0.10, 19.99, 100.50, 1234.56)

        for (amount in amounts) {
            val minor = Money.toMinorUnits(amount)
            assertEquals(amount, Money.toDouble(minor), 0.000_001)
        }
    }
}
