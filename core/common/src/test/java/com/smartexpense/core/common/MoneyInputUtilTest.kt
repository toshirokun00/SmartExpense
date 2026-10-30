package com.smartexpense.core.common

import com.smartexpense.core.common.util.MoneyInputUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyInputUtilTest {

    @Test
    fun formatsIntegerNumbersWithCommasWhileTyping() {
        assertEquals("1", MoneyInputUtil.formatAmountInput("1"))
        assertEquals("12", MoneyInputUtil.formatAmountInput("12"))
        assertEquals("123", MoneyInputUtil.formatAmountInput("123"))
        assertEquals("1,234", MoneyInputUtil.formatAmountInput("1234"))
        assertEquals("12,345", MoneyInputUtil.formatAmountInput("12345"))
        assertEquals("123,456", MoneyInputUtil.formatAmountInput("123456"))
        assertEquals("1,234,567", MoneyInputUtil.formatAmountInput("1234567"))
    }

    @Test
    fun formatsNumbersWithDecimalPointsWhileTyping() {
        assertEquals("1,234.", MoneyInputUtil.formatAmountInput("1234."))
        assertEquals("1,234.5", MoneyInputUtil.formatAmountInput("1234.5"))
        assertEquals("1,234.50", MoneyInputUtil.formatAmountInput("1234.50"))
    }

    @Test
    fun limitsDecimalsToTwoPlaces() {
        assertEquals("1,234.56", MoneyInputUtil.formatAmountInput("1234.567"))
        assertEquals("100.99", MoneyInputUtil.formatAmountInput("100.9999"))
    }

    @Test
    fun typingLeadingDecimalCreatesZeroPrefix() {
        assertEquals("0.", MoneyInputUtil.formatAmountInput("."))
        assertEquals("0.5", MoneyInputUtil.formatAmountInput(".5"))
        assertEquals("0.75", MoneyInputUtil.formatAmountInput(".75"))
    }

    @Test
    fun dropsLeadingZerosForNonZeroValues() {
        assertEquals("100", MoneyInputUtil.formatAmountInput("0100"))
        assertEquals("5", MoneyInputUtil.formatAmountInput("05"))
        assertEquals("0.05", MoneyInputUtil.formatAmountInput("0.05"))
        assertEquals("0", MoneyInputUtil.formatAmountInput("00"))
        assertEquals("0", MoneyInputUtil.formatAmountInput("0"))
    }

    @Test
    fun backspacingOverCommaDeletesPrecedingDigit() {
        // User had "1,234" and cursor was after the comma, pressing backspace deletes ','
        val formatted = MoneyInputUtil.formatAmountInput(
            input = "1234",
            previousInput = "1,234"
        )
        assertEquals("234", formatted)

        // Multiple commas: user had "1,234,567", deletes second comma
        val formatted2 = MoneyInputUtil.formatAmountInput(
            input = "1,234567",
            previousInput = "1,234,567"
        )
        assertEquals("123,567", formatted2)
    }

    @Test
    fun backspacingDigitAfterCommaRebalancesComma() {
        val formatted = MoneyInputUtil.formatAmountInput(
            input = "12,45",
            previousInput = "12,345"
        )
        assertEquals("1,245", formatted)
    }

    @Test
    fun emptyOrWhitespaceReturnsEmpty() {
        assertEquals("", MoneyInputUtil.formatAmountInput(""))
        assertEquals("", MoneyInputUtil.formatAmountInput("   "))
    }

    @Test
    fun preservesNonNumericInputsForDownstreamValidation() {
        assertEquals("abc", MoneyInputUtil.formatAmountInput("abc"))
    }

    @Test
    fun cleanAmountStripsCommas() {
        assertEquals("1234567.89", MoneyInputUtil.cleanAmount("1,234,567.89"))
        assertEquals("5000", MoneyInputUtil.cleanAmount("5,000"))
    }

    @Test
    fun parseAmountToDoubleParsesFormattedString() {
        assertEquals(1234.50, MoneyInputUtil.parseAmountToDouble("1,234.50")!!, 0.001)
        assertEquals(5000.0, MoneyInputUtil.parseAmountToDouble("5,000")!!, 0.001)
        assertNull(MoneyInputUtil.parseAmountToDouble(""))
        assertNull(MoneyInputUtil.parseAmountToDouble("0"))
        assertNull(MoneyInputUtil.parseAmountToDouble("-100"))
        assertNull(MoneyInputUtil.parseAmountToDouble("invalid"))
    }

    @Test
    fun extensionFunctionsWorkIdentically() {
        assertEquals("1,234.50", "1234.50".formatAsMoneyInput())
        assertEquals("1234.50", "1,234.50".cleanMoneyAmount())
    }

    @Test
    fun cursorPositionDoesNotJumpBeforeLastDigitWhenReachingFourDigits() {
        // User had "123" with cursor at 3, types '4' -> raw text "1234" with cursor at 4
        val result = MoneyInputUtil.formatAmountWithCursor(
            input = "1234",
            rawCursor = 4,
            previousInput = "123"
        )
        assertEquals("1,234", result.formatted)
        // Cursor must be after '4' (index 5), NOT before '4' (index 4)
        assertEquals(5, result.cursorPosition)
    }

    @Test
    fun cursorPositionFollowsSubsequentDigitsAndCommas() {
        // User types 5th digit: "1,234" + '5' -> "1,2345", cursor at 6
        val result5 = MoneyInputUtil.formatAmountWithCursor(
            input = "1,2345",
            rawCursor = 6,
            previousInput = "1,234"
        )
        assertEquals("12,345", result5.formatted)
        assertEquals(6, result5.cursorPosition)

        // User types 7th digit (adding a second comma): "123,456" + '7' -> "123,4567", cursor at 8
        val result7 = MoneyInputUtil.formatAmountWithCursor(
            input = "123,4567",
            rawCursor = 8,
            previousInput = "123,456"
        )
        assertEquals("1,234,567", result7.formatted)
        // 9 characters total, cursor after '7' is index 9
        assertEquals(9, result7.cursorPosition)
    }

    @Test
    fun cursorPositionWhenTypingDecimalPoint() {
        // User types '.' after "1,234"
        val resultDot = MoneyInputUtil.formatAmountWithCursor(
            input = "1,234.",
            rawCursor = 6,
            previousInput = "1,234"
        )
        assertEquals("1,234.", resultDot.formatted)
        assertEquals(6, resultDot.cursorPosition)

        // User types '.' on empty input -> becomes "0."
        val resultEmptyDot = MoneyInputUtil.formatAmountWithCursor(
            input = ".",
            rawCursor = 1,
            previousInput = ""
        )
        assertEquals("0.", resultEmptyDot.formatted)
        assertEquals(2, resultEmptyDot.cursorPosition)
    }

    @Test
    fun cursorPositionWhenTypingInTheMiddle() {
        // User had "1,234", placed cursor at index 2 (between ',' and '2'), types '9' -> "1,9234", cursor at 3
        val result = MoneyInputUtil.formatAmountWithCursor(
            input = "1,9234",
            rawCursor = 3,
            previousInput = "1,234"
        )
        assertEquals("19,234", result.formatted)
        // In "19,234", cursor should be right after '9' (index 2)
        assertEquals(2, result.cursorPosition)
    }
}
