package com.smartexpense.data

import com.smartexpense.data.remote.SupabaseBillRemoteDataSource
import com.smartexpense.data.repositoryimpl.BillRepositoryImpl
import com.smartexpense.data.sync.ExpenseSyncEnqueuer
import com.smartexpense.database.BillDeletionTransaction
import com.smartexpense.database.dao.BillDao
import com.smartexpense.database.dao.BillPaymentDao
import com.smartexpense.database.dao.PendingBillDeletionDao
import com.smartexpense.database.entity.BillEntity
import com.smartexpense.database.entity.BillPaymentEntity
import com.smartexpense.database.entity.SyncStatus
import com.smartexpense.domain.model.BillAmountType
import com.smartexpense.domain.model.BillFrequency
import com.smartexpense.domain.model.PaymentMethod
import com.smartexpense.domain.repository.AuthRepository
import com.smartexpense.domain.repository.CategoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

class BillRepositoryImplTest {

    private lateinit var billDao: BillDao
    private lateinit var billPaymentDao: BillPaymentDao
    private lateinit var supabaseBillRemoteDataSource: SupabaseBillRemoteDataSource
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var expenseSyncEnqueuer: ExpenseSyncEnqueuer
    private lateinit var pendingBillDeletionDao: PendingBillDeletionDao
    private lateinit var billDeletionTransaction: BillDeletionTransaction
    private lateinit var authRepository: AuthRepository
    private lateinit var repository: BillRepositoryImpl

    private val zoneOffset = ZoneOffset.UTC

    private fun millis(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day)
            .atStartOfDay(zoneOffset)
            .toInstant()
            .toEpochMilli()

    private fun billEntity(
        id: Long,
        dueDate: Long,
        frequency: BillFrequency,
        amountType: BillAmountType = BillAmountType.STATIC,
        amount: Double = 1200.0,
        isPaid: Boolean = false,
        skipped: Boolean = false
    ) = BillEntity(
        id = id,
        userId = "user-1",
        name = "Rent",
        amount = amount,
        dueDate = dueDate,
        isPaid = isPaid,
        frequency = frequency.name,
        amountType = amountType.name,
        skipped = skipped
    )

    @Before
    fun setup() {
        billDao = mockk()
        billPaymentDao = mockk()
        supabaseBillRemoteDataSource = mockk(relaxed = true)
        categoryRepository = mockk(relaxed = true)
        expenseSyncEnqueuer = mockk(relaxed = true)
        pendingBillDeletionDao = mockk(relaxed = true)
        billDeletionTransaction = mockk(relaxed = true)
        authRepository = mockk()
        repository = BillRepositoryImpl(
            billDao = billDao,
            billPaymentDao = billPaymentDao,
            supabaseBillRemoteDataSource = supabaseBillRemoteDataSource,
            categoryRepository = categoryRepository,
            expenseSyncEnqueuer = expenseSyncEnqueuer,
            pendingBillDeletionDao = pendingBillDeletionDao,
            billDeletionTransaction = billDeletionTransaction,
            authRepository = authRepository,
            clock = Clock.systemUTC()
        )

        coEvery { billPaymentDao.insertPayment(any()) } returns 1L
        coEvery { billPaymentDao.getRecentPaymentsForBill(any(), any()) } returns emptyList()
        coEvery { billDao.updateBill(any()) } returns Unit
    }

    @Test
    fun getBillsEmitsEmptyWhenSignedOut() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns null

        val bills = repository.getBills().first()

        assertTrue(bills.isEmpty())
    }

    @Test
    fun payBillRecordsPaymentAndAdvancesRecurringBill() = runTest {
        val entity = billEntity(
            id = 1L,
            dueDate = millis(2026, 1, 15),
            frequency = BillFrequency.MONTHLY
        )
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { billDao.getBillById(1L) } returns entity

        val paidAt = millis(2026, 1, 15)
        repository.payBill(1L, 1200.0, paidAt, PaymentMethod.CASH)

        coVerify {
            billPaymentDao.insertPayment(
                match {
                    it.billId == 1L &&
                        it.amount == 1200.0 &&
                        it.paidAt == paidAt
                }
            )
        }
        coVerify {
            billDao.updateBill(
                match {
                    it.dueDate == millis(2026, 2, 15) &&
                        !it.isPaid &&
                        it.paidAt == null
                }
            )
        }
    }

    @Test
    fun payBillVariableRecurringUsesRecentAverageAsNextEstimate() = runTest {
        val entity = billEntity(
            id = 2L,
            dueDate = millis(2026, 1, 15),
            frequency = BillFrequency.MONTHLY,
            amountType = BillAmountType.VARIABLE,
            amount = 1000.0
        )
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { billDao.getBillById(2L) } returns entity
        coEvery { billPaymentDao.getRecentPaymentsForBill(2L, 6) } returns listOf(
            BillPaymentEntity(
                userId = "user-1",
                billId = 2L,
                billName = "Rent",
                amount = 1000.0,
                paidAt = millis(2026, 1, 15),
                paymentMethod = "CASH",
                createdAt = millis(2026, 1, 15)
            ),
            BillPaymentEntity(
                userId = "user-1",
                billId = 2L,
                billName = "Rent",
                amount = 1400.0,
                paidAt = millis(2025, 12, 15),
                paymentMethod = "CASH",
                createdAt = millis(2025, 12, 15)
            )
        )

        repository.payBill(2L, 1000.0, millis(2026, 1, 15), PaymentMethod.CASH)

        coVerify {
            billDao.updateBill(
                match {
                    it.amount == 1200.0 &&
                        it.amountEstimated &&
                        it.dueDate == millis(2026, 2, 15)
                }
            )
        }
    }

    @Test
    fun payBillMarksOneTimeBillPaid() = runTest {
        val entity = billEntity(
            id = 3L,
            dueDate = millis(2026, 1, 20),
            frequency = BillFrequency.NONE,
            amount = 500.0
        )
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { billDao.getBillById(3L) } returns entity

        val paidAt = millis(2026, 1, 18)
        repository.payBill(3L, 550.0, paidAt, PaymentMethod.E_WALLET)

        coVerify {
            billDao.updateBill(
                match {
                    it.isPaid &&
                        it.paidAt == paidAt &&
                        it.amount == 550.0 &&
                        !it.amountEstimated
                }
            )
        }
    }

    @Test
    fun skipBillAdvancesRecurringBillWithoutPayment() = runTest {
        val entity = billEntity(
            id = 4L,
            dueDate = millis(2026, 3, 10),
            frequency = BillFrequency.MONTHLY
        )
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { billDao.getBillById(4L) } returns entity

        repository.skipBill(4L)

        coVerify {
            billDao.updateBill(
                match {
                    it.dueDate == millis(2026, 4, 10) && !it.isPaid
                }
            )
        }
        coVerify(exactly = 0) { billPaymentDao.insertPayment(any()) }
    }

    @Test
    fun skipBillMarksOneTimeBillSkipped() = runTest {
        val entity = billEntity(
            id = 5L,
            dueDate = millis(2026, 3, 10),
            frequency = BillFrequency.NONE
        )
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { billDao.getBillById(5L) } returns entity

        repository.skipBill(5L)

        coVerify {
            billDao.updateBill(match { it.skipped && !it.isPaid })
        }
    }

    @Test
    fun addBillRequiresSignedInUser() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns null

        try {
            repository.addBill(
                com.smartexpense.domain.model.Bill(
                    name = "Rent",
                    amount = 1000.0,
                    dueDateMillis = millis(2026, 1, 1)
                )
            )
            fail("Expected IllegalStateException")
        } catch (expected: IllegalStateException) {
            assertEquals("Sign in to add bills", expected.message)
        }
    }

    @Test
    fun deleteBillQueuesRemoteDeletionAndRemovesPaymentHistory() = runTest {
        val entity = billEntity(
            id = 8L,
            dueDate = millis(2026, 5, 1),
            frequency = BillFrequency.MONTHLY
        )
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { billDao.getBillById(8L) } returns entity

        repository.deleteBill(8L)

        coVerify {
            billDeletionTransaction.deleteBill(
                billId = 8L,
                clientId = entity.clientId
            )
        }
    }

    @Test
    fun getBillsMapsFrequencyAmountTypeAndNewFields() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { billDao.getBillsForUser("user-1") } returns flowOf(
            listOf(
                BillEntity(
                    id = 6L,
                    userId = "user-1",
                    name = "Water",
                    amount = 600.0,
                    dueDate = millis(2026, 3, 1),
                    frequency = BillFrequency.QUARTERLY.name,
                    repeatInterval = 2,
                    amountType = BillAmountType.VARIABLE.name,
                    amountEstimated = true,
                    categoryId = 9L,
                    payee = "Maynilad",
                    paymentMethod = PaymentMethod.E_WALLET.name,
                    autopay = true,
                    reminderDaysBefore = 5,
                    reminderTimeMinutes = 480,
                    skipped = true,
                    isActive = false
                )
            )
        )

        val bill = repository.getBills().first().single()

        assertEquals(BillFrequency.QUARTERLY, bill.frequency)
        assertEquals(2, bill.repeatInterval)
        assertEquals(BillAmountType.VARIABLE, bill.amountType)
        assertTrue(bill.amountEstimated)
        assertEquals(9L, bill.categoryId)
        assertEquals("Maynilad", bill.payee)
        assertEquals(PaymentMethod.E_WALLET, bill.paymentMethod)
        assertTrue(bill.autopay)
        assertEquals(5, bill.reminderDaysBefore)
        assertEquals(480, bill.reminderTimeMinutes)
        assertTrue(bill.skipped)
        assertEquals(false, bill.isActive)
    }

    @Test
    fun getPaymentHistoryEmitsEmptyWhenSignedOut() = runTest {
        coEvery { authRepository.getCurrentUserId() } returns null

        val payments = repository.getPaymentHistory().first()

        assertTrue(payments.isEmpty())
    }

    @Test
    fun getPaymentHistoryMapsEveryPaymentForTheUser() = runTest {
        val firstPaidAt = millis(2026, 2, 10)
        val secondPaidAt = millis(2026, 1, 10)
        coEvery { authRepository.getCurrentUserId() } returns "user-1"
        coEvery { billPaymentDao.getPaymentsForUser("user-1") } returns flowOf(
            listOf(
                BillPaymentEntity(
                    id = 2L,
                    userId = "user-1",
                    billId = 1L,
                    billName = "Meralco",
                    amount = 1250.0,
                    paidAt = secondPaidAt,
                    paymentMethod = PaymentMethod.E_WALLET.name,
                    createdAt = secondPaidAt
                ),
                BillPaymentEntity(
                    id = 1L,
                    userId = "user-1",
                    billId = 1L,
                    billName = "Meralco",
                    amount = 980.0,
                    paidAt = firstPaidAt,
                    paymentMethod = PaymentMethod.CASH.name,
                    createdAt = firstPaidAt
                )
            )
        )

        val payments = repository.getPaymentHistory().first()

        assertEquals(listOf(2L, 1L), payments.map { it.id })
        assertEquals("Meralco", payments.last().billName)
        assertEquals(980.0, payments.last().amount, 0.0)
        assertEquals(firstPaidAt, payments.last().paidAtMillis)
        assertEquals(PaymentMethod.CASH, payments.last().paymentMethod)
    }

    @Test
    fun syncBillsUploadsPendingPaymentsAfterTheirBillIsSynced() = runTest {
        val bill = billEntity(
            id = 1L,
            dueDate = millis(2026, 1, 15),
            frequency = BillFrequency.MONTHLY
        ).copy(syncStatus = SyncStatus.SYNCED)
        val payment = paymentEntity(
            id = 7L,
            billId = bill.id,
            clientId = "payment-a",
            amount = 980.0
        )
        coEvery { pendingBillDeletionDao.getPendingDeletions() } returns emptyList()
        coEvery { billDao.getUnsyncedBills() } returns emptyList()
        coEvery { billDao.getBillById(bill.id) } returns bill
        coEvery { billPaymentDao.getUnsyncedPayments() } returns listOf(payment)
        coEvery { billPaymentDao.updateSyncStatus(any(), any()) } returns Unit

        repository.syncBills("user-1")

        coVerify(exactly = 1) {
            supabaseBillRemoteDataSource.upsertBillPayment(
                match {
                    it.clientId == "payment-a" &&
                        it.userId == "user-1" &&
                        it.billClientId == bill.clientId &&
                        it.billName == "Meralco" &&
                        it.amount == 980.0 &&
                        it.paymentMethod == PaymentMethod.CASH.name
                }
            )
        }
        coVerify(exactly = 1) {
            billPaymentDao.updateSyncStatus(7L, SyncStatus.SYNCED.name)
        }
    }

    @Test
    fun syncBillsSkipsPaymentsUntilTheirBillIsSynced() = runTest {
        val bill = billEntity(
            id = 1L,
            dueDate = millis(2026, 1, 15),
            frequency = BillFrequency.MONTHLY
        ).copy(syncStatus = SyncStatus.PENDING)
        coEvery { pendingBillDeletionDao.getPendingDeletions() } returns emptyList()
        coEvery { billDao.getUnsyncedBills() } returns emptyList()
        coEvery { billDao.getBillById(bill.id) } returns bill
        coEvery { billPaymentDao.getUnsyncedPayments() } returns listOf(paymentEntity(billId = bill.id))

        repository.syncBills("user-1")

        coVerify(exactly = 0) {
            supabaseBillRemoteDataSource.upsertBillPayment(any())
        }
    }

    @Test
    fun syncBillsMarksPaymentFailedWhenUploadFails() = runTest {
        val bill = billEntity(
            id = 1L,
            dueDate = millis(2026, 1, 15),
            frequency = BillFrequency.MONTHLY
        ).copy(syncStatus = SyncStatus.SYNCED)
        val payment = paymentEntity(id = 9L, billId = bill.id)
        coEvery { pendingBillDeletionDao.getPendingDeletions() } returns emptyList()
        coEvery { billDao.getUnsyncedBills() } returns emptyList()
        coEvery { billDao.getBillById(bill.id) } returns bill
        coEvery { billPaymentDao.getUnsyncedPayments() } returns listOf(payment)
        coEvery { billPaymentDao.updateSyncStatus(any(), any()) } returns Unit
        coEvery {
            supabaseBillRemoteDataSource.upsertBillPayment(any())
        } throws RuntimeException("offline")

        try {
            repository.syncBills("user-1")
            fail("Expected IllegalStateException")
        } catch (expected: IllegalStateException) {
            assertEquals("Some bills failed to synchronize", expected.message)
        }

        coVerify(exactly = 1) {
            billPaymentDao.updateSyncStatus(9L, SyncStatus.FAILED.name)
        }
    }

    private fun paymentEntity(
        id: Long = 1L,
        billId: Long = 1L,
        clientId: String = "payment-a",
        userId: String = "user-1",
        amount: Double = 900.0,
        paidAt: Long = millis(2026, 1, 15),
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        createdAt: Long = paidAt
    ) = BillPaymentEntity(
        id = id,
        userId = userId,
        billId = billId,
        clientId = clientId,
        billName = "Meralco",
        amount = amount,
        paidAt = paidAt,
        paymentMethod = paymentMethod.name,
        createdAt = createdAt
    )
}
