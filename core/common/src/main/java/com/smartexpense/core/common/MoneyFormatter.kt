package com.smartexpense.core.common

import java.text.NumberFormat
import java.util.Locale

fun Double.toPesoAmount() : String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en","PH"))
    return formatter.format(this)
}