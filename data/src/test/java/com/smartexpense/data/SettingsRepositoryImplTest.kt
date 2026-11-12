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

    private fun stubSettings(
        thresholds: Set<Int>?,
        emailVisible: Boolean = true
    ) {
        every { dataStore.theme } returns flowOf("SYSTEM")
        every { dataStore.notificationsEnabled } returns flowOf(true)
        every { dataStore.budgetAlertThresholds } returns flowOf(thresholds)
        every { dataStore.emailVisible } returns flowOf(emailVisible)
        every { dataStore.spendoraId } returns flowOf("")
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

    @Test
    fun emailVisibilityIsReadFromSettings() = runTest {
        stubSettings(thresholds = null, emailVisible = false)

        val settings = SettingsRepositoryImpl(dataStore)
            .getSettings()
            .first()

        assertEquals(false, settings.isEmailVisible)
    }

    @Test
    fun settingEmailVisibilityPersistsIt() = runTest {
        coEvery { dataStore.setEmailVisible(any()) } just Runs

        SettingsRepositoryImpl(dataStore).setEmailVisible(false)

        coVerify(exactly = 1) { dataStore.setEmailVisible(false) }
    }

    @Test
    fun spendoraIdIsCachedInSettings() = runTest {
        stubSettings(thresholds = null)
        every { dataStore.spendoraId } returns flowOf("spendora42")

        val settings = SettingsRepositoryImpl(dataStore)
            .getSettings()
            .first()

        assertEquals("spendora42", settings.spendoraId)
    }

    @Test
    fun settingSpendoraIdPersistsIt() = runTest {
        coEvery { dataStore.setSpendoraId(any()) } just Runs

        SettingsRepositoryImpl(dataStore).setSpendoraId("spendora7")

        coVerify(exactly = 1) { dataStore.setSpendoraId("spendora7") }
    }
}
