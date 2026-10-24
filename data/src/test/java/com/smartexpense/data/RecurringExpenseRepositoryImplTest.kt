package com.smartexpense.data

import com.smartexpense.data.recurring.RecurringExpenseProcessor
import com.smartexpense.data.repositoryimpl.RecurringExpenseRepositoryImpl
import com.smartexpense.data.sync.ExpenseSyncEnqueuer
import com.smartexpense.data.sync.RecurringExpenseScheduler
import com.smartexpense.database.dao.RecurringScheduleDao
import com.smartexpense.database.entity.RecurringScheduleEntity
import com.smartexpense.domain.model.RecurrenceFrequency
import com.smartexpense.domain.model.RecurringExpenseSchedule
import com.smartexpense.domain.recurring.RecurringDateUtils
import com.smartexpense.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class RecurringExpenseRepositoryImplTest {

    private lateinit var scheduleDao: RecurringScheduleDao
    private lateinit var authRepository: AuthRepository
    private lateinit var processor: RecurringExpenseProcessor
    private lateinit var expenseSyncEnqueuer: ExpenseSyncEnqueuer
    private lateinit var scheduler: RecurringExpenseScheduler
    private lateinit var repository: RecurringExpenseRepositoryImpl

    private val zone = ZoneId.of("UTC")
    private val userId = "user-1"
    private val clock = Clock.fixed(
        Instant.parse("2026-02-15T12:00:00Z"),
        zone
    )

    @Before
    fun setUp() {
        scheduleDao = mockk(relaxed = true)
        authRepository = mockk()
        processor = mockk()
        expenseSyncEnqueuer = mockk(relaxed = true)
        scheduler = mockk(relaxed = true)

        repository = RecurringExpenseRepositoryImpl(
            scheduleDao = scheduleDao,
            authRepository = authRepository,
            processor = processor,
            expenseSyncEnqueuer = expenseSyncEnqueuer,
            scheduler = scheduler,
            clock = clock
        )
    }

    @Test
    fun `schedules are empty when signed out`() = runTest {
        coEvery {
            authRepository.getCurrentUserId()
        } returns null

        val schedules = repository.getSchedules().first()

        assertTrue(schedules.isEmpty())
        verify(exactly = 0) {
            scheduleDao.getSchedulesForUser(any())
        }
    }

    @Test
    fun `schedules are scoped to the signed in user`() = runTest {
        val entity = entity(
            id = 7L,
            userId = userId
        )

        coEvery {
            authRepository.getCurrentUserId()
        } returns userId

        every {
            scheduleDao.getSchedulesForUser(userId)
        } returns flowOf(listOf(entity))

        val schedules = repository.getSchedules().first()

        assertEquals(1, schedules.size)
        assertEquals(7L, schedules.single().id)
        assertEquals(userId, schedules.single().userId)
    }

    @Test
    fun `add schedule assigns owner normalises dates and enqueues work`() =
        runTest {
            coEvery {
                authRepository.getCurrentUserId()
            } returns userId

            coEvery {
                scheduleDao.insertScheduleIgnoringDuplicate(any())
            } returns 1L

            val start = RecurringDateUtils.toEpochMillis(
                date = LocalDate.of(2026, 1, 5),
                zoneId = zone
            ) + 5 * 60 * 60 * 1000L

            val schedule = RecurringExpenseSchedule(
                clientId = "schedule-client-id",
                description = "Rent",
                amount = 1000.0,
                categoryId = 1L,
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = start,
                nextOccurrence = start
            )

            val captured = slot<RecurringScheduleEntity>()

            coEvery {
                scheduleDao.insertScheduleIgnoringDuplicate(
                    capture(captured)
                )
            } returns 1L

            repository.addSchedule(schedule)

            assertEquals(userId, captured.captured.userId)
            assertEquals(
                RecurringDateUtils.toEpochMillis(
                    date = LocalDate.of(2026, 1, 5),
                    zoneId = zone
                ),
                captured.captured.startDate
            )
            assertEquals(
                captured.captured.startDate,
                captured.captured.nextOccurrence
            )
            assertEquals("schedule-client-id", captured.captured.clientId)
            assertTrue(captured.captured.isActive)

            verify {
                scheduler.enqueueImmediate()
            }
        }

    @Test
    fun `add schedule is idempotent when the client id already exists`() =
        runTest {
            coEvery {
                authRepository.getCurrentUserId()
            } returns userId

            coEvery {
                scheduleDao.insertScheduleIgnoringDuplicate(any())
            } returns -1L

            repository.addSchedule(
                RecurringExpenseSchedule(
                    clientId = "schedule-client-id",
                    description = "Rent",
                    amount = 1000.0,
                    categoryId = 1L,
                    frequency = RecurrenceFrequency.MONTHLY,
                    startDate = millis("2026-01-05"),
                    nextOccurrence = millis("2026-01-05")
                )
            )

            verify {
                scheduler.enqueueImmediate()
            }
        }

    @Test
    fun `get schedule by id hides another users schedule`() = runTest {
        coEvery {
            authRepository.getCurrentUserId()
        } returns userId

        coEvery {
            scheduleDao.getScheduleById(7L)
        } returns entity(
            id = 7L,
            userId = "user-2"
        )

        assertNull(repository.getScheduleById(7L))
    }

    @Test
    fun `update schedule rejects another users schedule`() = runTest {
        coEvery {
            authRepository.getCurrentUserId()
        } returns userId

        coEvery {
            scheduleDao.getScheduleById(7L)
        } returns entity(
            id = 7L,
            userId = "user-2"
        )

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                repository.updateSchedule(
                    schedule(
                        id = 7L,
                        start = "2026-01-05"
                    )
                )
            }
        }

        coVerify(exactly = 0) {
            scheduleDao.updateSchedule(any())
        }
    }

    @Test
    fun `update schedule recalculates next occurrence from the latest generated row`() =
        runTest {
            coEvery {
                authRepository.getCurrentUserId()
            } returns userId

            coEvery {
                scheduleDao.getScheduleById(7L)
            } returns entity(
                id = 7L,
                userId = userId,
                start = "2026-01-31",
                frequency = RecurrenceFrequency.MONTHLY,
                next = "2026-02-28"
            )

            coEvery {
                scheduleDao.getLatestOccurrenceDate(7L)
            } returns millis("2026-01-31")

            val captured = slot<RecurringScheduleEntity>()

            coEvery {
                scheduleDao.updateSchedule(capture(captured))
            } returns Unit

            repository.updateSchedule(
                schedule(
                    id = 7L,
                    start = "2026-01-31",
                    frequency = RecurrenceFrequency.MONTHLY,
                    amount = 1200.0
                )
            )

            assertEquals(1200.0, captured.captured.amount, 0.0)
            assertEquals(
                millis("2026-02-28"),
                captured.captured.nextOccurrence
            )
            assertEquals(userId, captured.captured.userId)
            assertEquals(
                "schedule-client-id",
                captured.captured.clientId
            )

            verify {
                scheduler.enqueueImmediate()
            }
        }

    @Test
    fun `pause marks the schedule inactive without changing next occurrence`() =
        runTest {
            coEvery {
                authRepository.getCurrentUserId()
            } returns userId

            coEvery {
                scheduleDao.getScheduleById(7L)
            } returns entity(
                id = 7L,
                userId = userId,
                start = "2026-01-05",
                next = "2026-01-12"
            )

            repository.setActive(
                id = 7L,
                isActive = false
            )

            coVerify {
                scheduleDao.setActive(
                    id = 7L,
                    isActive = false
                )
            }

            coVerify(exactly = 0) {
                scheduleDao.updateSchedule(any())
            }

            verify(exactly = 0) {
                scheduler.enqueueImmediate()
            }
        }

    @Test
    fun `resume skips occurrences that became due during the pause`() =
        runTest {
            coEvery {
                authRepository.getCurrentUserId()
            } returns userId

            coEvery {
                scheduleDao.getScheduleById(7L)
            } returns entity(
                id = 7L,
                userId = userId,
                start = "2026-01-05",
                next = "2026-01-12",
                isActive = false
            )

            val captured = slot<RecurringScheduleEntity>()

            coEvery {
                scheduleDao.updateSchedule(capture(captured))
            } returns Unit

            repository.setActive(
                id = 7L,
                isActive = true
            )

            assertTrue(captured.captured.isActive)
            assertEquals(
                millis("2026-02-16"),
                captured.captured.nextOccurrence
            )

            verify {
                scheduler.enqueueImmediate()
            }
        }

    @Test
    fun `delete only removes the signed in users schedule`() = runTest {
        coEvery {
            authRepository.getCurrentUserId()
        } returns userId

        val otherUsersSchedule = entity(
            id = 7L,
            userId = "user-2"
        )

        coEvery {
            scheduleDao.getScheduleById(7L)
        } returns otherUsersSchedule

        repository.deleteSchedule(7L)

        coVerify(exactly = 0) {
            scheduleDao.deleteSchedule(any())
        }
    }

    @Test
    fun `processing returns zero when signed out`() = runTest {
        coEvery {
            authRepository.getCurrentUserId()
        } returns null

        val generated = repository.processDueSchedules()

        assertEquals(0, generated)
        coVerify(exactly = 0) {
            scheduleDao.getDueSchedules(any(), any())
        }
        verify(exactly = 0) {
            expenseSyncEnqueuer.enqueue()
        }
    }

    @Test
    fun `processing generates due occurrences and enqueues expense sync`() =
        runTest {
            val dueSchedule = entity(
                id = 7L,
                userId = userId,
                start = "2026-01-05",
                next = "2026-01-12"
            )

            coEvery {
                authRepository.getCurrentUserId()
            } returns userId

            coEvery {
                scheduleDao.getDueSchedules(
                    userId = userId,
                    nowMillis = any()
                )
            } returns listOf(dueSchedule)

            coEvery {
                processor.process(
                    scheduleId = any(),
                    userId = any(),
                    today = any(),
                    zoneId = any()
                )
            } returns 2

            val generated = repository.processDueSchedules()

            assertEquals(2, generated)

            coVerify {
                processor.process(
                    scheduleId = 7L,
                    userId = userId,
                    today = LocalDate.of(2026, 2, 15),
                    zoneId = zone
                )
            }

            verify {
                expenseSyncEnqueuer.enqueue()
            }
        }

    @Test
    fun `processing does not enqueue sync when nothing is generated`() =
        runTest {
            val dueSchedule = entity(
                id = 7L,
                userId = userId
            )

            coEvery {
                authRepository.getCurrentUserId()
            } returns userId

            coEvery {
                scheduleDao.getDueSchedules(
                    userId = userId,
                    nowMillis = any()
                )
            } returns listOf(dueSchedule)

            coEvery {
                processor.process(
                    scheduleId = any(),
                    userId = any(),
                    today = any(),
                    zoneId = any()
                )
            } returns 0

            val generated = repository.processDueSchedules()

            assertEquals(0, generated)
            verify(exactly = 0) {
                expenseSyncEnqueuer.enqueue()
            }
        }

    @Test
    fun `processing enqueues sync even when a later schedule fails`() =
        runTest {
            val first = entity(
                id = 7L,
                userId = userId
            )
            val second = entity(
                id = 8L,
                userId = userId
            )

            coEvery {
                authRepository.getCurrentUserId()
            } returns userId

            coEvery {
                scheduleDao.getDueSchedules(
                    userId = userId,
                    nowMillis = any()
                )
            } returns listOf(first, second)

            coEvery {
                processor.process(
                    scheduleId = 7L,
                    userId = any(),
                    today = any(),
                    zoneId = any()
                )
            } returns 1

            coEvery {
                processor.process(
                    scheduleId = 8L,
                    userId = any(),
                    today = any(),
                    zoneId = any()
                )
            } throws IllegalStateException("boom")

            var failure: Throwable? = null

            try {
                repository.processDueSchedules()
            } catch (throwable: Throwable) {
                failure = throwable
            }

            assertTrue(failure is IllegalStateException)

            verify {
                expenseSyncEnqueuer.enqueue()
            }
        }

    private fun schedule(
        id: Long,
        start: String,
        frequency: RecurrenceFrequency =
            RecurrenceFrequency.WEEKLY,
        amount: Double = 1000.0
    ): RecurringExpenseSchedule {
        return RecurringExpenseSchedule(
            id = id,
            clientId = "schedule-client-id",
            userId = userId,
            description = "Rent",
            amount = amount,
            categoryId = 1L,
            frequency = frequency,
            startDate = millis(start),
            nextOccurrence = millis(start),
            isActive = true
        )
    }

    private fun entity(
        id: Long,
        userId: String,
        start: String = "2026-01-05",
        frequency: RecurrenceFrequency = RecurrenceFrequency.WEEKLY,
        next: String = start,
        isActive: Boolean = true
    ): RecurringScheduleEntity {
        return RecurringScheduleEntity(
            id = id,
            clientId = "schedule-client-id",
            userId = userId,
            description = "Rent",
            amount = 1000.0,
            categoryId = 1L,
            frequency = frequency.name,
            interval = 1,
            startDate = millis(start),
            endDate = null,
            nextOccurrence = millis(next),
            isActive = isActive
        )
    }

    private fun millis(value: String): Long {
        return RecurringDateUtils.toEpochMillis(
            date = LocalDate.parse(value),
            zoneId = zone
        )
    }
}
