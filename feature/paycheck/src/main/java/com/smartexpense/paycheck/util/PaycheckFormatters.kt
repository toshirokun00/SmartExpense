package com.smartexpense.paycheck.util

import com.smartexpense.core.common.util.MoneyInputUtil

internal fun formatAmountForEditing(amount: Double): String {
    val raw = if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()
    return MoneyInputUtil.formatAmountInput(raw)
}
