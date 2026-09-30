package com.smartexpense.domain

import com.smartexpense.domain.usecase.ValidateAmountUseCase
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ValidateAmountUseCaseTest {

    private lateinit var useCase : ValidateAmountUseCase

    @Before
    fun setup() {
        useCase = ValidateAmountUseCase()
    }

    @Test
    fun validAmountReturnsDoubleValue() {
        val result = useCase("100.50")

        Assert.assertEquals(100.50, result ?: error("Expected a valid amount"), 0.001)
    }

    @Test
    fun zeroAmountReturnsNull() {
        val result = useCase("0")

        Assert.assertNull(result)
    }

    @Test
    fun negativeAmountReturnNull() {
        val result = useCase("-100")

        Assert.assertNull(result)
    }

    @Test
    fun invalidAmountReturnsNull() {
        val result = useCase("abc")

        Assert.assertNull(result)
    }

    @Test
    fun emptyAmountReturnsNull() {
        val result = useCase("")

        Assert.assertNull(result)
    }

    @Test
    fun amountWithWithSpaceReturnsNull() {
        val result = useCase("  ")

        Assert.assertNull(result)

    }

}