package com.smartexpense.core.common.util

/**
 * Utility for formatting and handling money amount inputs in real-time as users type.
 * Automatically inserts thousands separators (commas) into the integer portion, restricts
 * decimal digits to two places, and gracefully handles typing and backspacing.
 */
object MoneyInputUtil {

    private const val MAX_DECIMAL_PLACES = 2

    /**
     * Formats [input] with comma thousands separators as it is being typed.
     *
     * @param input The current text entered by the user.
     * @param previousInput The previous text in the field, used to intelligently detect
     *   when a user backspaces over a comma to remove the preceding digit instead of getting stuck.
     * @return The formatted amount string with comma separators.
     *
     * Examples:
     * - `formatAmountInput("1234")` -> `"1,234"`
     * - `formatAmountInput("1234.")` -> `"1,234."`
     * - `formatAmountInput("1234.5")` -> `"1,234.5"`
     * - `formatAmountInput("1234.56")` -> `"1,234.56"`
     * - `formatAmountInput(".")` -> `"0."`
     * - `formatAmountInput(".5")` -> `"0.5"`
     */
    fun formatAmountInput(input: String, previousInput: String? = null): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""

        // Preserve non-numeric inputs (e.g. test mocks or invalid text) so validation can handle them.
        if (trimmed.any { it !in '0'..'9' && it != ',' && it != '.' }) {
            return trimmed
        }

        if (previousInput == "0." && trimmed == ".") {
            return ""
        }

        // Handle backspace when user deletes a comma
        if (previousInput != null &&
            previousInput.length - trimmed.length == 1 &&
            previousInput.count { it == ',' } > trimmed.count { it == ',' } &&
            previousInput.filter { it.isDigit() } == trimmed.filter { it.isDigit() }
        ) {
            var diffIdx = 0
            while (diffIdx < trimmed.length && previousInput[diffIdx] == trimmed[diffIdx]) {
                diffIdx++
            }
            if (diffIdx in 1 until previousInput.length && previousInput[diffIdx] == ',') {
                val adjusted = previousInput.removeRange(diffIdx - 1, diffIdx + 1)
                return formatAmountInput(adjusted)
            }
        }

        val hasDecimal = trimmed.contains('.')

        val beforeDecimal: String
        val afterDecimal: String?

        if (hasDecimal) {
            val parts = trimmed.split('.', limit = 2)
            beforeDecimal = parts[0]
            afterDecimal = parts.getOrNull(1)?.filter { it.isDigit() }?.take(MAX_DECIMAL_PLACES) ?: ""
        } else {
            beforeDecimal = trimmed
            afterDecimal = null
        }

        val rawDigits = beforeDecimal.filter { it.isDigit() }

        val integerPart = if (rawDigits.isEmpty()) {
            if (hasDecimal) "0" else ""
        } else {
            rawDigits.dropWhile { it == '0' }.ifEmpty { "0" }
        }

        if (integerPart.isEmpty() && afterDecimal == null) {
            return ""
        }

        val formattedInteger = formatWithCommas(integerPart)

        return when {
            hasDecimal && afterDecimal != null -> "$formattedInteger.$afterDecimal"
            hasDecimal -> "$formattedInteger."
            else -> formattedInteger
        }
    }

    /**
     * Result of formatting an amount input with its corresponding cursor position.
     */
    data class FormattedAmountResult(
        val formatted: String,
        val cursorPosition: Int
    )

    /**
     * Formats [input] with commas while accurately recalculating the [rawCursor] position
     * so the cursor never jumps unexpectedly (e.g., when the 4th digit inserts a comma).
     */
    fun formatAmountWithCursor(
        input: String,
        rawCursor: Int,
        previousInput: String? = null
    ): FormattedAmountResult {
        val formatted = formatAmountInput(input, previousInput)
        val cursor = calculateCursorPosition(
            rawText = input,
            rawCursor = rawCursor,
            formattedText = formatted
        )
        return FormattedAmountResult(
            formatted = formatted,
            cursorPosition = cursor
        )
    }

    /**
     * Calculates the new cursor position in [formattedText] based on the number of
     * digits and decimal points that were before [rawCursor] in [rawText].
     */
    fun calculateCursorPosition(
        rawText: String,
        rawCursor: Int,
        formattedText: String
    ): Int {
        if (formattedText.isEmpty()) return 0
        val clampedCursor = rawCursor.coerceIn(0, rawText.length)
        val textBeforeCursor = rawText.take(clampedCursor)
        val digitsBeforeCursor = textBeforeCursor.count { it.isDigit() }
        val decimalBeforeCursor = textBeforeCursor.contains('.')

        if (digitsBeforeCursor == 0 && decimalBeforeCursor && formattedText.startsWith("0.")) {
            return 2.coerceAtMost(formattedText.length)
        }

        var digitsCounted = 0
        var decimalCounted = false
        var targetCursor = 0

        for (i in formattedText.indices) {
            if (digitsCounted == digitsBeforeCursor && (!decimalBeforeCursor || decimalCounted)) {
                targetCursor = i
                break
            }
            val char = formattedText[i]
            if (char.isDigit()) {
                digitsCounted++
            } else if (char == '.') {
                decimalCounted = true
            }
            targetCursor = i + 1
        }

        return targetCursor.coerceIn(0, formattedText.length)
    }

    /**
     * Strips commas and extraneous whitespace from a formatted amount string,
     * leaving a clean decimal string suitable for numeric conversion.
     */
    fun cleanAmount(formatted: String): String {
        return formatted.replace(",", "").trim()
    }

    /**
     * Parses a formatted or unformatted money string into a positive [Double],
     * returning `null` if the string is empty, invalid, or represents a non-positive value.
     */
    fun parseAmountToDouble(input: String): Double? {
        val clean = cleanAmount(input)
        val value = clean.toDoubleOrNull()
        return if (value != null && value > 0) value else null
    }

    private fun formatWithCommas(digits: String): String {
        if (digits.isEmpty()) return ""
        val reversed = digits.reversed()
        val chunks = reversed.chunked(3)
        return chunks.joinToString(",").reversed()
    }
}
