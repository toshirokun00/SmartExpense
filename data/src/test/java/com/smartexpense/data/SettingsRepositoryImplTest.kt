package com.smartexpense.data

import com.smartexpense.core.common.SettingsDataStore
import com.smartexpense.data.repositoryimpl.SettingsRepositoryImpl
import com.smartexpense.domain.model.BudgetAlertThresholds
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsRepositoryImplTest {

    private val dataStore = mockk<SettingsDataStore>()

    private fun stubSettings(thresholds: Set<Int>?) {
        every { dataStore.theme } returns flowOf("SYSTEM")
        every { dataStore.notificationsEnabled } returns flowOf(true)
        every { dataStore.budgetAlertThresholds } returns flowOf(thresholds)
    }

    @Test
    fun unsetThresholdsUseDefaults() = runTest {
        stubSettings(thresholds = null)

        val settings = SettingsRepositoryImpl(dataStore)
            .getSettings()
            .first()

        assertEquals(
            BudgetAlertThresholds.DEFAULT,
            settings.budgetAlertThresholds
        )
    }

    @Test
    fun explicitlyEmptyThresholdsArePreserved() = runTest {
        stubSettings(thresholds = emptySet())

        val settings = SettingsRepositoryImpl(dataStore)
            .getSettings()
            .first()

        assertEquals(emptySet<Int>(), settings.budgetAlertThresholds)
    }

    @Test
    fun unsupportedThresholdsAreFiltered() = runTest {
        stubSettings(thresholds = setOf(50, 75))

        val settings = SettingsRepositoryImpl(dataStore)
            .getSettings()
            .first()

        assertEquals(setOf(50), settings.budgetAlertThresholds)
    }

    @Test
    fun settingEmptyThresholdsPersistsEmptySet() = runTest {
        coEvery { dataStore.setBudgetAlertThresholds(any()) } just Runs

        SettingsRepositoryImpl(dataStore)
            .setBudgetAlertThresholds(emptySet())

        coVerify(exactly = 1) {
            dataStore.setBudgetAlertThresholds(emptySet())
        }
    }
}
