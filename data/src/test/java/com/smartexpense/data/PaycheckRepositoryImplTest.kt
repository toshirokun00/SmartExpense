package com.smartexpense.data

import com.smartexpense.data.remote.SupabasePaycheckRemoteDataSource
import com.smartexpense.data.repositoryimpl.PaycheckRepositoryImpl
import com.smartexpense.data.sync.ExpenseSyncEnqueuer
import com.smartexpense.database.dao.BillDao
import com.smartexpense.database.dao.PaycheckBillDao
import com.smartexpense.database.dao.PaycheckDao
import com.smartexpense.database.dao.PaycheckScheduleDao
import com.smartexpense.database.dao.PendingPaycheckDeletionDao
import com.smartexpense.database.entity.BillEntity
import com.smartexpense.database.entity.PaycheckBillEntity
import com.smartexpense.database.entity.PaycheckEntity
import com.smartexpense.database.entity.PaycheckScheduleEntity
import com.smartexpense.database.entity.PendingPaycheckDeletionEntity
import com.smartexpense.database.entity.SyncStatus
import com.smartexpense.domain.model.BillFrequency
import com.smartexpense.domain.model.Paycheck
import com.smartexpense.domain.model.PaycheckFrequency
import com.smartexpense.domain.model.PaycheckSchedule
import com.smartexpense.domain.model.paycheckOccurrenceClientId
import com.smartexpense.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class PaycheckRepositoryImplTest {

    private lateinit var paycheckDao: PaycheckDao
    private lateinit var paycheckBillDao: PaycheckBillDao
    private lateinit var paycheckScheduleDao: PaycheckScheduleDao
    private lateinit var pendingPaycheckDeletionDao: PendingPaycheckDeletionDao
    private lateinit var remoteDataSource: SupabasePaycheckRemoteDataSource
    private lateinit var billDao: BillDao
    private lateinit var expenseSyncEnqueuer: ExpenseSyncEnqueuer
    private lateinit var authRepository: AuthRepository
    private lateinit var repository: PaycheckRepositoryImpl

    private val clock = Clock.fixed(
        Instant.parse("2026-11-10T12:00:00Z"),
        ZoneOffset.UTC
    )

    private fun bill(
        id: Long,
        clientId: String,
        frequency: BillFrequency = BillFrequency.NONE,
        isActive: Boolean = true,
        skipped: Boolean = false
    ) = BillEntity(
        id = id,
        userId = "user-1",
        clientId = clientId,
        name = "Bill $id",
        amount = 100.0,
        dueDate = 0L,
        frequency = frequency.name,
        isActive = isActive,
        skipped = skipped
    )

    private fun assignment(
        id: Long,
        paycheckId: Long,
        billId: Long
    ) = PaycheckBillEntity(
        id = id,
        userId = "user-1",
        paycheckId = paycheckId,
        billId = billId,
        clientId = "assignment-$id",
        paycheckClientId = "pc-$paycheckId",
        billClientId = "bill-$billId",
        createdAt = 700L
    )

    private fun paycheckEntity(id: Long = 5L, clientId: String = "pc-1") =
        PaycheckEntity(
            id = id,
            userId = "user-1",
            clientId = clientId,
            amount = 2_000.0,
            payDate = 1_000L,
            label = "Pay",
            createdAt = 500L
        )

    @Before
    fun setup() {
        paycheckDao = mockk(relaxed = true)
        paycheckBillDao = mockk(relaxed = true)
        paycheckScheduleDao = mockk(relaxed = true)
        pendingPaycheckDeletionDao = mockk(relaxed = true)
        remoteDataSource = mockk(relaxed = true)
        billDao = mockk(relaxed = true)
        expenseSyncEnqueuer = mockk(relaxed = true)
        authRepository = mockk()
        repository = PaycheckRepositoryImpl(
            paycheckDao = paycheckDao,
            paycheckBillDao = paycheckBillDao,
            paycheckScheduleDao = paycheckScheduleDao,
            pendingPaycheckDeletionDao = pendingPaycheckDeletionDao,
            supabasePaycheckRemoteDataSource = remoteDataSource,
            billDao = billDao,
            expenseSyncEnqueuer = expenseSyncEnqueuer,
            authRepository = authRepository,
            clock = clock
        )

        coEvery { paycheckDao.insertPaycheck(any()) } returns 5L
        coEvery { pendingPaycheckDeletionDao.getPendingDeletions() } returns emptyList()
        coEvery { paycheckDao.getUnsyncedPaychecks() } returns emptyList()
        coEvery { paycheckScheduleDao.getUnsyncedSchedules() } returns emptyList()
        coEvery { paycheckScheduleDao.getActiveSchedulesForUser(any()) } returns emptyList()
    }

    @Test
    fun `paychecks are empty when signed out`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns null

        assertTrue(repository.getPaychecks().first().isEmpty())
    }

    @Test
    fun `add inserts the paycheck and its bill assignments`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { billDao.getBillById(1L) } returns bill(1L, "bill-1")
        coEvery { billDao.getBillById(2L) } returns bill(2L, "bill-2")

        val id = repository.addPaycheck(
            Paycheck(
                amount = 2_000.0,
                payDateMillis = 1_000L,
                label = "Pay",
                billIds = listOf(1L, 2L)
            )
        )

        assertEquals(5L, id)
        coVerify(exactly = 1) {
            paycheckDao.insertPaycheck(
                match {
                    it.userId == "user-1" &&
                        it.amount == 2_000.0 &&
                        it.payDate == 1_000L &&
                        it.label == "Pay" &&
                        it.createdAt == clock.millis()
                }
            )
        }
        coVerify(exactly = 1) {
            paycheckBillDao.insertAssignment(
                match {
                    it.paycheckId == 5L &&
                        it.billId == 1L &&
                        it.billClientId == "bill-1"
                }
            )
        }
        coVerify(exactly = 1) {
            paycheckBillDao.insertAssignment(
                match {
                    it.paycheckId == 5L &&
                        it.billId == 2L &&
                        it.billClientId == "bill-2"
                }
            )
        }
    }

    @Test
    fun `add ignores bills owned by another user`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { billDao.getBillById(1L) } returns bill(1L, "bill-1")
            .copy(userId = "user-2")

        repository.addPaycheck(
            Paycheck(
                amount = 2_000.0,
                payDateMillis = 1_000L,
                billIds = listOf(1L)
            )
        )

        coVerify(exactly = 0) { paycheckBillDao.insertAssignment(any()) }
    }

    @Test
    fun `update keeps the existing assignment client ids`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { paycheckDao.getPaycheckById(5L) } returns paycheckEntity()
        coEvery { paycheckBillDao.getAssignmentsForPaycheck(5L) } returns listOf(
            PaycheckBillEntity(
                id = 20L,
                userId = "user-1",
                paycheckId = 5L,
                billId = 1L,
                clientId = "assignment-1",
                paycheckClientId = "pc-1",
                billClientId = "bill-1",
                createdAt = 700L
            )
        )
        coEvery { billDao.getBillById(1L) } returns bill(1L, "bill-1")
        coEvery { billDao.getBillById(2L) } returns bill(2L, "bill-2")

        repository.updatePaycheck(
            Paycheck(
                id = 5L,
                amount = 2_500.0,
                payDateMillis = 2_000L,
                label = "Updated",
                billIds = listOf(1L, 2L)
            )
        )

        coVerify(exactly = 1) {
            paycheckDao.updatePaycheck(
                match {
                    it.id == 5L &&
                        it.amount == 2_500.0 &&
                        it.payDate == 2_000L &&
                        it.label == "Updated" &&
                        it.syncStatus == SyncStatus.PENDING
                }
            )
        }
        coVerify(exactly = 1) { paycheckBillDao.deleteAssignmentsForPaycheck(5L) }
        coVerify(exactly = 1) {
            paycheckBillDao.insertAssignment(
                match { it.billId == 1L && it.clientId == "assignment-1" }
            )
        }
        coVerify(exactly = 1) {
            paycheckBillDao.insertAssignment(
                match { it.billId == 2L && it.clientId != "assignment-1" }
            )
        }
    }

    @Test
    fun `delete queues a remote deletion and removes local rows`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { paycheckDao.getPaycheckById(5L) } returns paycheckEntity()

        repository.deletePaycheck(5L)

        coVerify(exactly = 1) {
            pendingPaycheckDeletionDao.insert(
                PendingPaycheckDeletionEntity(clientId = "pc-1")
            )
        }
        coVerify(exactly = 1) { paycheckBillDao.deleteAssignmentsForPaycheck(5L) }
        coVerify(exactly = 1) { paycheckDao.deletePaycheckById(5L) }
    }

    @Test
    fun `sync uploads the paycheck and its assignments`() = runTest {
        coEvery { paycheckDao.getUnsyncedPaychecks() } returns listOf(paycheckEntity())
        coEvery { paycheckBillDao.getUnsyncedForPaycheck(5L) } returns listOf(
            PaycheckBillEntity(
                id = 20L,
                userId = "user-1",
                paycheckId = 5L,
                billId = 1L,
                clientId = "assignment-1",
                paycheckClientId = "pc-1",
                billClientId = "bill-1",
                createdAt = 700L
            )
        )
        coEvery { billDao.getBillById(1L) } returns bill(1L, "bill-1")

        repository.syncPaychecks("user-1")

        coVerify(exactly = 1) {
            remoteDataSource.upsertPaycheck(
                match {
                    it.clientId == "pc-1" &&
                        it.userId == "user-1" &&
                        it.amount == 2_000.0 &&
                        it.label == "Pay"
                }
            )
        }
        coVerify(exactly = 1) {
            remoteDataSource.deletePaycheckBills("pc-1")
        }
        coVerify(exactly = 1) {
            remoteDataSource.upsertPaycheckBill(
                match {
                    it.clientId == "assignment-1" &&
                        it.paycheckClientId == "pc-1" &&
                        it.billClientId == "bill-1"
                }
            )
        }
        coVerify(exactly = 1) {
            paycheckDao.updateSyncStatus(5L, SyncStatus.SYNCED.name)
        }
    }

    @Test
    fun `sync applies queued deletions before uploading`() = runTest {
        coEvery { pendingPaycheckDeletionDao.getPendingDeletions() } returns listOf(
            PendingPaycheckDeletionEntity(clientId = "old-pc")
        )

        repository.syncPaychecks("user-1")

        coVerify(exactly = 1) {
            remoteDataSource.deletePaycheckByClientId("old-pc")
        }
        coVerify(exactly = 1) {
            pendingPaycheckDeletionDao.deleteByClientId("old-pc")
        }
    }

    @Test
    fun `failed upload marks the paycheck as failed`() = runTest {
        coEvery { paycheckDao.getUnsyncedPaychecks() } returns listOf(paycheckEntity())
        coEvery { remoteDataSource.upsertPaycheck(any()) } throws
                IllegalStateException("network")

        val result = runCatching { repository.syncPaychecks("user-1") }

        assertTrue(result.isFailure)
        coVerify(exactly = 1) {
            paycheckDao.updateSyncStatus(5L, SyncStatus.FAILED.name)
        }
    }

    @Test
    fun `recurring schedule materializes every cutoff for the planning horizon`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { paycheckScheduleDao.insertScheduleIgnoringDuplicate(any()) } returns 7L
        val captured = mutableListOf<PaycheckEntity>()
        coEvery {
            paycheckDao.insertPaycheckIgnoringDuplicate(capture(captured))
        } returns 1L

        repository.addPaycheckSchedule(
            PaycheckSchedule(
                clientId = "schedule-1",
                amount = 2_000.0,
                label = "Salary",
                frequency = PaycheckFrequency.SEMI_MONTHLY,
                firstDay = 15,
                secondDay = 30,
                startDateMillis = dateMillis(2026, 11, 15)
            )
        )

        // 15th and 30th from November through April, with February clamped.
        assertEquals(12, captured.size)
        val dates = captured.map { it.payDate.toLocalDate() }
        assertEquals(
            listOf(
                "2026-11-15", "2026-11-30",
                "2026-12-15", "2026-12-30",
                "2027-01-15", "2027-01-30",
                "2027-02-15", "2027-02-28",
                "2027-03-15", "2027-03-30",
                "2027-04-15", "2027-04-30"
            ),
            dates.map { it.toString() }
        )
        captured.forEach { occurrence ->
            assertEquals("schedule-1", occurrence.scheduleClientId)
            assertEquals(occurrence.payDate, occurrence.occurrenceDate)
            assertEquals(2_000.0, occurrence.amount, 0.001)
            assertEquals("Salary", occurrence.label)
            assertEquals(SyncStatus.PENDING, occurrence.syncStatus)
            assertEquals(
                paycheckOccurrenceClientId("schedule-1", occurrence.payDate.toLocalDate()),
                occurrence.clientId
            )
        }
    }

    @Test
    fun `recurring schedule assigns the form selection to the first cutoff only`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { paycheckScheduleDao.insertScheduleIgnoringDuplicate(any()) } returns 7L
        coEvery { paycheckDao.insertPaycheckIgnoringDuplicate(any()) } returns 1L
        coEvery { billDao.getBillById(1L) } returns bill(1L, "bill-1")
        coEvery { billDao.getBillById(2L) } returns bill(2L, "bill-2")

        repository.addPaycheckSchedule(
            schedule = PaycheckSchedule(
                clientId = "schedule-1",
                amount = 2_000.0,
                frequency = PaycheckFrequency.SEMI_MONTHLY,
                firstDay = 15,
                secondDay = 30,
                startDateMillis = dateMillis(2026, 11, 15)
            ),
            billIds = listOf(1L, 2L)
        )

        val firstOccurrenceClientId = paycheckOccurrenceClientId(
            scheduleClientId = "schedule-1",
            date = LocalDate.of(2026, 11, 15)
        )
        coVerify(exactly = 1) {
            paycheckBillDao.insertAssignment(
                match {
                    it.paycheckId == 1L &&
                        it.billId == 1L &&
                        it.paycheckClientId == firstOccurrenceClientId
                }
            )
        }
        coVerify(exactly = 1) {
            paycheckBillDao.insertAssignment(
                match {
                    it.paycheckId == 1L &&
                        it.billId == 2L &&
                        it.paycheckClientId == firstOccurrenceClientId
                }
            )
        }
        // Later cutoffs start with no bills, so the two bills above stay on
        // the first paycheck only.
        coVerify(exactly = 2) { paycheckBillDao.insertAssignment(any()) }
    }

    @Test
    fun `later cutoffs of a new schedule start without bill assignments`() =
        runTest {
            coEvery { authRepository.getCurrentUserId() } returns "user-1"
            coEvery { paycheckScheduleDao.insertScheduleIgnoringDuplicate(any()) } returns 7L
            var nextPaycheckId = 100L
            coEvery { paycheckDao.insertPaycheckIgnoringDuplicate(any()) } answers {
                nextPaycheckId++
            }
            coEvery { billDao.getBillById(1L) } returns
                bill(1L, "bill-1", BillFrequency.MONTHLY)
            coEvery { billDao.getBillById(2L) } returns bill(2L, "bill-2")

            val assignments = mutableListOf<PaycheckBillEntity>()
            coEvery { paycheckBillDao.insertAssignment(capture(assignments)) } returns 1L

            repository.addPaycheckSchedule(
                schedule = PaycheckSchedule(
                    clientId = "schedule-1",
                    amount = 2_000.0,
                    frequency = PaycheckFrequency.SEMI_MONTHLY,
                    firstDay = 15,
                    secondDay = 30,
                    startDateMillis = dateMillis(2026, 11, 15)
                ),
                billIds = listOf(1L, 2L)
            )

            val firstCutoffClientId = paycheckOccurrenceClientId(
                scheduleClientId = "schedule-1",
                date = LocalDate.of(2026, 11, 15)
            )

            // Only the first cutoff gets the selection; the 11 later cutoffs
            // start empty so no bill is covered twice.
            assertEquals(2, assignments.size)
            assertTrue(
                assignments.all { it.paycheckClientId == firstCutoffClientId }
            )
        }

    @Test
    fun `new occurrences start without bill assignments`() = runTest {
        coEvery { paycheckScheduleDao.getActiveSchedulesForUser("user-1") } returns
            listOf(scheduleEntity())
        var nextPaycheckId = 100L
        coEvery { paycheckDao.insertPaycheckIgnoringDuplicate(any()) } answers {
            nextPaycheckId++
        }

        val assignments = mutableListOf<PaycheckBillEntity>()
        coEvery { paycheckBillDao.insertAssignment(capture(assignments)) } returns 1L

        repository.syncPaychecks("user-1")

        coVerify(exactly = 12) { paycheckDao.insertPaycheckIgnoringDuplicate(any()) }
        coVerify(exactly = 0) { paycheckBillDao.insertAssignment(any()) }
    }

    @Test
    fun `updating a cutoff leaves later cutoffs of the same schedule unchanged`() =
        runTest {
            coEvery { authRepository.getCurrentUserId() } returns "user-1"
            coEvery { paycheckDao.getPaycheckById(5L) } returns paycheckEntity().copy(
                scheduleClientId = "schedule-1",
                payDate = dateMillis(2026, 11, 15),
                occurrenceDate = dateMillis(2026, 11, 15)
            )
            coEvery { paycheckBillDao.getAssignmentsForPaycheck(5L) } returns listOf(
                assignment(id = 20L, paycheckId = 5L, billId = 1L)
            )
            coEvery { billDao.getBillById(1L) } returns bill(1L, "bill-1")
            coEvery { billDao.getBillById(4L) } returns
                bill(4L, "bill-4", BillFrequency.MONTHLY)

            repository.updatePaycheck(
                Paycheck(
                    id = 5L,
                    amount = 2_000.0,
                    payDateMillis = dateMillis(2026, 11, 15),
                    billIds = listOf(1L, 4L)
                )
            )

            // The edit only rewrites assignments on the edited paycheck itself;
            // other cutoffs keep whatever they cover.
            coVerify(exactly = 1) {
                paycheckBillDao.insertAssignment(
                    match { it.paycheckId == 5L && it.billId == 4L }
                )
            }
            coVerify(exactly = 1) { paycheckDao.updatePaycheck(any()) }
        }

    @Test
    fun `recurring schedule does not materialize occurrences after its end date`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { paycheckScheduleDao.insertScheduleIgnoringDuplicate(any()) } returns 7L
        val captured = mutableListOf<PaycheckEntity>()
        coEvery {
            paycheckDao.insertPaycheckIgnoringDuplicate(capture(captured))
        } returns 1L

        repository.addPaycheckSchedule(
            PaycheckSchedule(
                clientId = "schedule-1",
                amount = 2_000.0,
                frequency = PaycheckFrequency.SEMI_MONTHLY,
                firstDay = 15,
                secondDay = 30,
                startDateMillis = dateMillis(2026, 11, 15),
                endDateMillis = dateMillis(2026, 12, 31)
            )
        )

        assertEquals(
            listOf("2026-11-15", "2026-11-30", "2026-12-15", "2026-12-30"),
            captured.map { it.payDate.toLocalDate().toString() }
        )
    }

    @Test
    fun `adding a schedule with an existing client id reuses the stored row`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { paycheckScheduleDao.insertScheduleIgnoringDuplicate(any()) } returns -1L
        coEvery { paycheckScheduleDao.getScheduleByClientId("schedule-1") } returns
            scheduleEntity()

        val id = repository.addPaycheckSchedule(
            PaycheckSchedule(
                clientId = "schedule-1",
                amount = 2_000.0,
                startDateMillis = dateMillis(2026, 11, 15)
            )
        )

        assertEquals(7L, id)
        coVerify(exactly = 0) { paycheckDao.insertPaycheckIgnoringDuplicate(any()) }
    }

    @Test
    fun `stop repeating deactivates the schedule and keeps existing paychecks`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { paycheckScheduleDao.getScheduleByClientId("schedule-1") } returns
            scheduleEntity()

        repository.stopPaycheckSchedule("schedule-1")

        coVerify(exactly = 1) {
            paycheckScheduleDao.updateSchedule(
                match {
                    it.clientId == "schedule-1" &&
                        !it.isActive &&
                        it.syncStatus == SyncStatus.PENDING
                }
            )
        }
        coVerify(exactly = 0) { paycheckDao.deletePaycheckById(any()) }
    }

    @Test
    fun `sync uploads recurring schedules before paychecks`() = runTest {
        coEvery { paycheckScheduleDao.getUnsyncedSchedules() } returns listOf(
            scheduleEntity()
        )

        repository.syncPaychecks("user-1")

        coVerify(exactly = 1) {
            remoteDataSource.upsertPaycheckSchedule(
                match {
                    it.clientId == "schedule-1" &&
                        it.userId == "user-1" &&
                        it.frequency == "SEMI_MONTHLY" &&
                        it.firstDay == 15 &&
                        it.secondDay == 30 &&
                        it.isActive
                }
            )
        }
        coVerify(exactly = 1) {
            paycheckScheduleDao.updateSyncStatus(7L, SyncStatus.SYNCED.name)
        }
    }

    @Test
    fun `sync materializes upcoming cutoffs for active schedules`() = runTest {
        coEvery { paycheckScheduleDao.getActiveSchedulesForUser("user-1") } returns
            listOf(scheduleEntity())

        repository.syncPaychecks("user-1")

        coVerify(exactly = 12) { paycheckDao.insertPaycheckIgnoringDuplicate(any()) }
    }

    @Test
    fun `sync uploads recurring occurrence fields`() = runTest {
        val occurrenceDate = dateMillis(2026, 11, 15)
        coEvery { paycheckDao.getUnsyncedPaychecks() } returns listOf(
            paycheckEntity().copy(
                scheduleClientId = "schedule-1",
                occurrenceDate = occurrenceDate
            )
        )

        repository.syncPaychecks("user-1")

        coVerify(exactly = 1) {
            remoteDataSource.upsertPaycheck(
                match {
                    it.scheduleClientId == "schedule-1" &&
                        it.occurrenceDate == Instant
                            .ofEpochMilli(occurrenceDate)
                            .toString()
                }
            )
        }
    }

    @Test
    fun `deleting a recurring occurrence keeps a tombstone instead of deleting it`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { paycheckDao.getPaycheckById(6L) } returns paycheckEntity(
            id = 6L,
            clientId = "pc-occurrence"
        ).copy(
            scheduleClientId = "schedule-1",
            occurrenceDate = dateMillis(2026, 11, 15)
        )

        repository.deletePaycheck(6L)

        coVerify(exactly = 1) {
            paycheckDao.updatePaycheck(
                match {
                    it.id == 6L &&
                        it.isSkipped &&
                        it.syncStatus == SyncStatus.PENDING
                }
            )
        }
        coVerify(exactly = 0) { pendingPaycheckDeletionDao.insert(any()) }
        coVerify(exactly = 0) { paycheckDao.deletePaycheckById(any()) }
        coVerify(exactly = 1) { paycheckBillDao.deleteAssignmentsForPaycheck(6L) }
    }

    @Test
    fun `sync uploads the tombstone for a deleted recurring occurrence`() = runTest {
        coEvery { paycheckDao.getUnsyncedPaychecks() } returns listOf(
            paycheckEntity().copy(
                scheduleClientId = "schedule-1",
                occurrenceDate = dateMillis(2026, 11, 15),
                isSkipped = true
            )
        )

        repository.syncPaychecks("user-1")

        coVerify(exactly = 1) {
            remoteDataSource.upsertPaycheck(
                match { it.scheduleClientId == "schedule-1" && it.isSkipped }
            )
        }
    }

    private fun scheduleEntity(isActive: Boolean = true) = PaycheckScheduleEntity(
        id = 7L,
        userId = "user-1",
        clientId = "schedule-1",
        amount = 2_000.0,
        label = "Salary",
        frequency = PaycheckFrequency.SEMI_MONTHLY.name,
        firstDay = 15,
        secondDay = 30,
        startDate = dateMillis(2026, 11, 15),
        isActive = isActive,
        createdAt = 500L
    )

    private fun dateMillis(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

    private fun Long.toLocalDate(): LocalDate =
        Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

    @Test
    fun `getPaychecks combines paychecks with their assigned bill ids`() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { paycheckDao.getPaychecksForUser("user-1") } returns flowOf(
            listOf(paycheckEntity())
        )
        coEvery { paycheckBillDao.getAssignmentsForUser("user-1") } returns flowOf(
            listOf(
                PaycheckBillEntity(
                    id = 20L,
                    userId = "user-1",
                    paycheckId = 5L,
                    billId = 1L,
                    clientId = "assignment-1",
                    paycheckClientId = "pc-1",
                    billClientId = "bill-1",
                    createdAt = 700L
                )
            )
        )

        val paychecks = repository.getPaychecks().first()

        assertEquals(1, paychecks.size)
        assertEquals(listOf(1L), paychecks.first().billIds)
    }
}
