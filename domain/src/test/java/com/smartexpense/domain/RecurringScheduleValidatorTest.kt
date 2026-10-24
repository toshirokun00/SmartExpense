package com.smartexpense.domain

import com.smartexpense.domain.model.RecurrenceFrequency
import com.smartexpense.domain.model.RecurringExpenseSchedule
import com.smartexpense.domain.recurring.RecurringScheduleValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecurringScheduleValidatorTest {

    @Test
    fun `valid schedule passes`() {
        assertNull(RecurringScheduleValidator.validate(validSchedule()))
    }

    @Test
    fun `blank description is rejected`() {
        val message = RecurringScheduleValidator.validate(
            validSchedule().copy(description = "   ")
        )

        assertEquals("Please enter a description", message)
    }

    @Test
    fun `zero amount is rejected`() {
        val message = RecurringScheduleValidator.validate(
            validSchedule().copy(amount = 0.0)
        )

        assertEquals("Please enter a valid amount", message)
    }

    @Test
    fun `negative amount is rejected`() {
        val message = RecurringScheduleValidator.validate(
            validSchedule().copy(amount = -10.0)
        )

        assertEquals("Please enter a valid amount", message)
    }

    @Test
    fun `non finite amount is rejected`() {
        val message = RecurringScheduleValidator.validate(
            validSchedule().copy(amount = Double.NaN)
        )

        assertEquals("Please enter a valid amount", message)
    }

    @Test
    fun `missing category is rejected`() {
        val message = RecurringScheduleValidator.validate(
            validSchedule().copy(categoryId = 0L)
        )

        assertEquals("Please select a category", message)
    }

    @Test
    fun `zero interval is rejected`() {
        val message = RecurringScheduleValidator.validate(
            validSchedule().copy(interval = 0)
        )

        assertEquals("Repeat interval must be at least 1", message)
    }

    @Test
    fun `excessive interval is rejected`() {
        val message = RecurringScheduleValidator.validate(
            validSchedule().copy(
                interval = RecurringScheduleValidator.MAX_INTERVAL + 1
            )
        )

        assertEquals(
            "Repeat interval must be 52 or less",
            message
        )
    }

    @Test
    fun `end date before start date is rejected`() {
        val message = RecurringScheduleValidator.validate(
            validSchedule().copy(
                startDate = 2_000L,
                endDate = 1_000L
            )
        )

        assertEquals("End date cannot be before start date", message)
    }

    @Test
    fun `end date equal to start date is allowed`() {
        assertNull(
            RecurringScheduleValidator.validate(
                validSchedule().copy(
                    startDate = 1_000L,
                    endDate = 1_000L
                )
            )
        )
    }

    private fun validSchedule(): RecurringExpenseSchedule {
        return RecurringExpenseSchedule(
            clientId = "schedule-client-id",
            userId = "user-1",
            description = "Rent",
            amount = 1000.0,
            categoryId = 1L,
            frequency = RecurrenceFrequency.MONTHLY,
            interval = 1,
            startDate = 1_000L,
            endDate = null,
            nextOccurrence = 1_000L
        )
    }
}
