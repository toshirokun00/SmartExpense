package com.smartexpense.profile

import com.smartexpense.profile.util.HIDDEN_EMAIL
import com.smartexpense.profile.util.maskEmail
import org.junit.Assert.assertEquals
import org.junit.Test

class EmailVisibilityTest {

    @Test
    fun `visible email is returned unchanged`() {
        assertEquals(
            "malvin@example.com",
            maskEmail("malvin@example.com", isVisible = true)
        )
    }

    @Test
    fun `hidden email is replaced with asterisks`() {
        assertEquals(HIDDEN_EMAIL, maskEmail("malvin@example.com", isVisible = false))
    }

    @Test
    fun `blank email stays blank when hidden`() {
        assertEquals("", maskEmail("", isVisible = false))
    }
}
