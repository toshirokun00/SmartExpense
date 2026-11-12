package com.smartexpense.data

import app.cash.turbine.test
import com.smartexpense.data.remote.SupabaseProfileRemoteDataSource
import com.smartexpense.data.repositoryimpl.SupabaseProfileRemoteRepository
import com.smartexpense.domain.repository.ProfileRemoteRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SupabaseProfileRemoteRepositoryTest {

    private val dataSource = mockk<SupabaseProfileRemoteDataSource>()
    private val repository = SupabaseProfileRemoteRepository(dataSource)

    @Test
    fun `loadProfile maps remote data to domain model`() = runTest {
        coEvery { dataSource.loadProfile() } returns
                SupabaseProfileRemoteDataSource.ProfileData(
                    username = "Malvin",
                    avatarUri = "https://cdn/avatar.jpg",
                    spendoraId = "spendora42",
                    isEmailVisible = false
                )

        val profile = repository.loadProfile()

        assertEquals("Malvin", profile?.username)
        assertEquals("https://cdn/avatar.jpg", profile?.avatarUri)
        assertEquals("spendora42", profile?.spendoraId)
        assertEquals(false, profile?.isEmailVisible)
    }

    @Test
    fun `loadProfile returns null when remote has no data`() = runTest {
        coEvery { dataSource.loadProfile() } returns null

        assertNull(repository.loadProfile())
    }

    @Test
    fun `saveProfile delegates and publishes the saved profile`() = runTest {
        coEvery { dataSource.saveProfile(any(), any(), any()) } returns null
        coEvery { dataSource.loadProfile() } returns
                SupabaseProfileRemoteDataSource.ProfileData("Malvin", null)

        repository.usernameChanged.test {
            repository.saveProfile(username = "Malvin", avatarUri = null)

            val emitted = awaitItem()
            assertEquals("Malvin", emitted.username)
            assertNull(emitted.avatarUri)

            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) {
            dataSource.saveProfile("Malvin", null, false)
        }
    }

    @Test
    fun `saveProfile publishes nothing when remote profile is empty`() = runTest {
        coEvery { dataSource.saveProfile(any(), any(), any()) } returns null
        coEvery { dataSource.loadProfile() } returns null

        repository.usernameChanged.test {
            repository.saveProfile(username = null, avatarUri = null, deleteAvatar = true)

            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `spendora id is loaded and saved through the data source`() = runTest {
        coEvery { dataSource.loadSpendoraId() } returns "spendora7"
        coEvery { dataSource.saveSpendoraId(any()) } returns Unit

        assertEquals("spendora7", repository.loadSpendoraId())

        repository.saveSpendoraId("spendora8")

        coVerify(exactly = 1) { dataSource.saveSpendoraId("spendora8") }
    }

    @Test
    fun `email visibility is loaded and saved through the data source`() = runTest {
        coEvery { dataSource.loadEmailVisibility() } returns false
        coEvery { dataSource.saveEmailVisibility(any()) } returns Unit

        assertEquals(false, repository.loadEmailVisibility())

        repository.saveEmailVisibility(true)

        coVerify(exactly = 1) { dataSource.saveEmailVisibility(true) }
    }

    @Test
    fun `usernameChanged does not replay a previous account's profile`() =
        runTest {
            // User A saves a profile.
            coEvery { dataSource.saveProfile(any(), any(), any()) } returns null
            coEvery { dataSource.loadProfile() } returns
                    SupabaseProfileRemoteDataSource.ProfileData("UserA", null)

            repository.saveProfile(username = "UserA", avatarUri = null)

            // User B starts observing: the previous profile must NOT replay.
            repository.usernameChanged.test {
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }
}
