package com.smartexpense.data

import com.smartexpense.data.repositoryimpl.ProfileRepositoryImpl
import com.smartexpense.database.dao.UserProfileDao
import com.smartexpense.database.entity.UserProfileEntity
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
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileRepositoryImplTest {

    private val dao = mockk<UserProfileDao>()
    private val repository = ProfileRepositoryImpl(dao)

    @Test
    fun `observeProfile maps entity to domain model`() = runTest {
        every { dao.getProfileForUser("user-1") } returns flowOf(
            UserProfileEntity(
                userId = "user-1",
                username = "Malvin",
                avatarUri = "https://cdn/avatar.jpg"
            )
        )

        val profile = repository.observeProfile("user-1").first()

        assertEquals("user-1", profile?.userId)
        assertEquals("Malvin", profile?.username)
        assertEquals("https://cdn/avatar.jpg", profile?.avatarUri)
    }

    @Test
    fun `observeProfile emits null when there is no cached profile`() = runTest {
        every { dao.getProfileForUser("user-1") } returns flowOf(null)

        assertNull(repository.observeProfile("user-1").first())
    }

    @Test
    fun `saveProfile upserts the entity`() = runTest {
        coEvery { dao.upsertProfile(any()) } just Runs

        repository.saveProfile(
            userId = "user-1",
            username = "Malvin",
            avatarUri = "https://cdn/avatar.jpg"
        )

        coVerify(exactly = 1) {
            dao.upsertProfile(
                UserProfileEntity(
                    userId = "user-1",
                    username = "Malvin",
                    avatarUri = "https://cdn/avatar.jpg"
                )
            )
        }
    }

    @Test
    fun `deleteProfile deletes only the current user's row`() = runTest {
        coEvery { dao.deleteProfileForUser(any()) } just Runs

        repository.deleteProfile("user-1")

        coVerify(exactly = 1) { dao.deleteProfileForUser("user-1") }
    }
}
