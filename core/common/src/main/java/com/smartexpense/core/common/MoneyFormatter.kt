package com.smartexpense.core.common

import com.smartexpense.core.common.util.MoneyInputUtil
import java.text.NumberFormat
import java.util.Locale

fun Double.toPesoAmount() : String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en","PH"))
    return formatter.format(this)
}

/**
 * Formats a money input string with commas while typing.
 */
fun String.formatAsMoneyInput(previousInput: String? = null): String =
    MoneyInputUtil.formatAmountInput(this, previousInput)

/**
 * Strips commas and extraneous whitespace from a money string.
 */
fun String.cleanMoneyAmount(): String =
    MoneyInputUtil.cleanAmount(this)
