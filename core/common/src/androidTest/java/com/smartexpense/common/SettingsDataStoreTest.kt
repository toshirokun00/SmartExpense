package com.smartexpense.common

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smartexpense.core.common.SettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsDataStoreTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val dataStore = SettingsDataStore(context)

    @Test
    fun emptyThresholdsRoundTripAsEmpty() = runTest {
        dataStore.setBudgetAlertThresholds(setOf(90))
        assertEquals(
            setOf(90),
            dataStore.budgetAlertThresholds.first()
        )

        dataStore.setBudgetAlertThresholds(emptySet())
        assertEquals(
            emptySet<Int>(),
            dataStore.budgetAlertThresholds.first()
        )
    }

    @Test
    fun nonEmptyThresholdsRoundTrip() = runTest {
        dataStore.setBudgetAlertThresholds(setOf(50, 80, 100))
        assertEquals(
            setOf(50, 80, 100),
            dataStore.budgetAlertThresholds.first()
        )
    }
}
