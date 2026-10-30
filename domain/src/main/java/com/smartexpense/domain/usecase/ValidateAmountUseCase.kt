package com.smartexpense.domain.usecase

class ValidateAmountUseCase {

    operator fun invoke(amount: String): Double? {
        val clean = amount.replace(",", "").trim()
        val value = clean.toDoubleOrNull()

        return if (value != null && value > 0) {
            value
        } else {
            null
        }
    }
}