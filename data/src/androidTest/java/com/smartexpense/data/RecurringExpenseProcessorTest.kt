package com.smartexpense.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartexpense.data.recurring.RecurringExpenseProcessor
import com.smartexpense.database.AppDatabase
import com.smartexpense.database.entity.RecurringScheduleEntity
import com.smartexpense.database.entity.SyncStatus
import com.smartexpense.domain.model.RecurrenceFrequency
import com.smartexpense.domain.recurring.RecurringDateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class RecurringExpenseProcessorTest {

    private lateinit var database: AppDatabase
    private lateinit var processor: RecurringExpenseProcessor

    private val zone = ZoneId.of("UTC")
    private val userId = "user-1"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        processor = RecurringExpenseProcessor(database)
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    @Test
    fun generatesOneExpensePerDueDateWithStableIdsAndAssociation() =
        runBlocking {
            val scheduleId = insertSchedule(
                start = "2026-01-05",
                frequency = RecurrenceFrequency.WEEKLY
            )

            val generated = processor.process(
                scheduleId = scheduleId,
                userId = userId,
                today = date("2026-01-19"),
                zoneId = zone
            )

            val expenses = expenses()

            assertEquals(3, generated)
            assertEquals(3, expenses.size)
            assertEquals(
                listOf(
                    "2026-01-05",
                    "2026-01-12",
                    "2026-01-19"
                ),
                expenses.map { occurrenceDate(it) }
            )
            assertTrue(
                expenses.all { expense ->
                    expense.userId == userId &&
                        expense.recurringScheduleId == scheduleId &&
                        expense.syncStatus == SyncStatus.PENDING &&
                        expense.description == "Rent" &&
                        expense.amount == 1000.0 &&
                        expense.categoryId == 1L
                }
            )
            assertEquals(
                listOf(
                    "recurring-schedule-client-id-2026-01-05",
                    "recurring-schedule-client-id-2026-01-12",
                    "recurring-schedule-client-id-2026-01-19"
                ),
                expenses.map { it.clientId }
            )
            assertEquals(
                millis("2026-01-26"),
                schedule(scheduleId).nextOccurrence
            )
        }

    @Test
    fun duplicateWorkerExecutionIsIdempotent() = runBlocking {
        val scheduleId = insertSchedule(
            start = "2026-01-05",
            frequency = RecurrenceFrequency.WEEKLY
        )

        val firstRun = processor.process(
            scheduleId = scheduleId,
            userId = userId,
            today = date("2026-01-19"),
            zoneId = zone
        )

        val secondRun = processor.process(
            scheduleId = scheduleId,
            userId = userId,
            today = date("2026-01-19"),
            zoneId = zone
        )

        assertEquals(3, firstRun)
        assertEquals(0, secondRun)
        assertEquals(3, expenses().size)
        assertEquals(
            millis("2026-01-26"),
            schedule(scheduleId).nextOccurrence
        )
    }

    @Test
    fun laterProcessingCatchesUpOnlyNewlyDueOccurrences() =
        runBlocking {
            val scheduleId = insertSchedule(
                start = "2026-01-05",
                frequency = RecurrenceFrequency.WEEKLY
            )

            processor.process(
                scheduleId = scheduleId,
                userId = userId,
                today = date("2026-01-12"),
                zoneId = zone
            )

            val secondRun = processor.process(
                scheduleId = scheduleId,
                userId = userId,
                today = date("2026-01-26"),
                zoneId = zone
            )

            assertEquals(2, secondRun)
            assertEquals(
                listOf(
                    "2026-01-05",
                    "2026-01-12",
                    "2026-01-19",
                    "2026-01-26"
                ),
                expenses().map { occurrenceDate(it) }
            )
            assertEquals(
                millis("2026-02-02"),
                schedule(scheduleId).nextOccurrence
            )
        }

    @Test
    fun pausedScheduleGeneratesNothing() = runBlocking {
        val scheduleId = insertSchedule(
            start = "2026-01-05",
            frequency = RecurrenceFrequency.WEEKLY,
            isActive = false
        )

        val generated = processor.process(
            scheduleId = scheduleId,
            userId = userId,
            today = date("2026-02-01"),
            zoneId = zone
        )

        assertEquals(0, generated)
        assertTrue(expenses().isEmpty())
        assertEquals(
            millis("2026-01-05"),
            schedule(scheduleId).nextOccurrence
        )
    }

    @Test
    fun deletedScheduleGeneratesNothing() = runBlocking {
        val scheduleId = insertSchedule(
            start = "2026-01-05",
            frequency = RecurrenceFrequency.WEEKLY
        )

        val entity = schedule(scheduleId)
        database.recurringScheduleDao().deleteSchedule(entity)

        val generated = processor.process(
            scheduleId = scheduleId,
            userId = userId,
            today = date("2026-02-01"),
            zoneId = zone
        )

        assertEquals(0, generated)
        assertTrue(expenses().isEmpty())
    }

    @Test
    fun anotherUserCannotProcessTheSchedule() = runBlocking {
        val scheduleId = insertSchedule(
            start = "2026-01-05",
            frequency = RecurrenceFrequency.WEEKLY
        )

        val generated = processor.process(
            scheduleId = scheduleId,
            userId = "user-2",
            today = date("2026-02-01"),
            zoneId = zone
        )

        assertEquals(0, generated)
        assertTrue(expenses().isEmpty())
        assertEquals(
            millis("2026-01-05"),
            schedule(scheduleId).nextOccurrence
        )
    }

    @Test
    fun endDateStopsGenerationAfterTheFinalOccurrence() =
        runBlocking {
            val scheduleId = insertSchedule(
                start = "2026-01-05",
                frequency = RecurrenceFrequency.WEEKLY,
                end = "2026-01-19"
            )

            val generated = processor.process(
                scheduleId = scheduleId,
                userId = userId,
                today = date("2026-12-31"),
                zoneId = zone
            )

            assertEquals(3, generated)
            assertEquals(
                listOf(
                    "2026-01-05",
                    "2026-01-12",
                    "2026-01-19"
                ),
                expenses().map { occurrenceDate(it) }
            )
            assertEquals(
                millis("2026-01-26"),
                schedule(scheduleId).nextOccurrence
            )
        }

    @Test
    fun monthlyGenerationClampsMonthEnds() = runBlocking {
        val scheduleId = insertSchedule(
            start = "2026-01-31",
            frequency = RecurrenceFrequency.MONTHLY
        )

        val generated = processor.process(
            scheduleId = scheduleId,
            userId = userId,
            today = date("2026-03-31"),
            zoneId = zone
        )

        assertEquals(3, generated)
        assertEquals(
            listOf(
                "2026-01-31",
                "2026-02-28",
                "2026-03-31"
            ),
            expenses().map { occurrenceDate(it) }
        )
        assertEquals(
            millis("2026-04-30"),
            schedule(scheduleId).nextOccurrence
        )
    }

    @Test
    fun yearlyGenerationHandlesFebruary29() = runBlocking {
        val scheduleId = insertSchedule(
            start = "2024-02-29",
            frequency = RecurrenceFrequency.YEARLY
        )

        val generated = processor.process(
            scheduleId = scheduleId,
            userId = userId,
            today = date("2025-03-01"),
            zoneId = zone
        )

        assertEquals(2, generated)
        assertEquals(
            listOf(
                "2024-02-29",
                "2025-02-28"
            ),
            expenses().map { occurrenceDate(it) }
        )
        assertEquals(
            millis("2026-02-28"),
            schedule(scheduleId).nextOccurrence
        )
    }

    @Test
    fun expenseInsertionFailureRollsBackTheWholeOccurrence() =
        runBlocking {
            val scheduleId = insertSchedule(
                start = "2026-01-05",
                frequency = RecurrenceFrequency.WEEKLY
            )

            database.openHelper.writableDatabase.execSQL(
                """
                CREATE TRIGGER fail_expense_insert
                BEFORE INSERT ON expenses
                BEGIN
                    SELECT RAISE(ABORT, 'forced failure');
                END
                """.trimIndent()
            )

            var failure: Throwable? = null

            try {
                processor.process(
                    scheduleId = scheduleId,
                    userId = userId,
                    today = date("2026-01-19"),
                    zoneId = zone
                )
            } catch (throwable: Throwable) {
                failure = throwable
            }

            assertNotNull(failure)
            assertTrue(expenses().isEmpty())
            assertEquals(
                millis("2026-01-05"),
                schedule(scheduleId).nextOccurrence
            )
        }

    private suspend fun insertSchedule(
        start: String,
        frequency: RecurrenceFrequency,
        interval: Int = 1,
        end: String? = null,
        next: String = start,
        isActive: Boolean = true,
        scheduleUserId: String = userId,
        clientId: String = "schedule-client-id"
    ): Long {
        return database.recurringScheduleDao()
            .insertScheduleIgnoringDuplicate(
                RecurringScheduleEntity(
                    clientId = clientId,
                    userId = scheduleUserId,
                    description = "Rent",
                    amount = 1000.0,
                    categoryId = 1L,
                    frequency = frequency.name,
                    interval = interval,
                    startDate = millis(start),
                    endDate = end?.let(::millis),
                    nextOccurrence = millis(next),
                    isActive = isActive
                )
            )
    }

    private suspend fun schedule(
        id: Long
    ): RecurringScheduleEntity {
        return database.recurringScheduleDao().getScheduleById(id)
            ?: error("Schedule $id was not found")
    }

    private suspend fun expenses() = database.expenseDao()
        .getExpenses()
        .first()
        .sortedBy { expense ->
            expense.occurrenceDate ?: Long.MAX_VALUE
        }

    private fun occurrenceDate(
        expense: com.smartexpense.database.entity.ExpenseEntity
    ): String {
        return RecurringDateUtils
            .toLocalDate(
                epochMillis = expense.occurrenceDate
                    ?: error("Occurrence date is missing"),
                zoneId = zone
            )
            .toString()
    }

    private fun date(value: String): LocalDate {
        return LocalDate.parse(value)
    }

    private fun millis(value: String): Long {
        return RecurringDateUtils.toEpochMillis(
            date = date(value),
            zoneId = zone
        )
    }
}
