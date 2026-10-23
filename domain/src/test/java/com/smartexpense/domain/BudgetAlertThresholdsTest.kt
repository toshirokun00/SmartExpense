package com.smartexpense.domain

import com.smartexpense.domain.model.BudgetAlertThresholds
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetAlertThresholdsTest {

    @Test
    fun normalizeKeepsEmptyConfiguration() {
        assertEquals(
            emptySet<Int>(),
            BudgetAlertThresholds.normalize(emptySet())
        )
    }

    @Test
    fun normalizeDropsUnsupportedThresholds() {
        assertEquals(
            setOf(50, 100),
            BudgetAlertThresholds.normalize(setOf(50, 75, 100))
        )
    }

    @Test
    fun normalizeKeepsSupportedThresholds() {
        assertEquals(
            setOf(50, 80, 90, 100),
            BudgetAlertThresholds.normalize(setOf(50, 80, 90, 100))
        )
    }

    @Test
    fun warningThresholdFallsBackWhenNoThresholdsConfigured() {
        assertEquals(
            BudgetAlertThresholds.WARNING,
            BudgetAlertThresholds.warningThreshold(emptySet())
        )
    }
}
