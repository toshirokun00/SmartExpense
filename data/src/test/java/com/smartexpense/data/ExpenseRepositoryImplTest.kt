package com.smartexpense.data

import com.smartexpense.data.remote.SupabaseExpenseRemoteDataSource
import com.smartexpense.data.repositoryimpl.ExpenseRepositoryImpl
import com.smartexpense.data.sync.ExpenseSyncEnqueuer
import com.smartexpense.data.sync.RemoteDataSync
import com.smartexpense.database.ExpenseDeletionTransaction
import com.smartexpense.database.dao.ExpenseDao
import com.smartexpense.database.dao.PendingExpenseDeletionDao
import com.smartexpense.database.entity.ExpenseEntity
import com.smartexpense.database.entity.SyncStatus
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.repository.CategoryRepository
import io.github.jan.supabase.SupabaseClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ExpenseRepositoryImplTest {

    private lateinit var expenseDao: ExpenseDao
    private lateinit var supabaseExpenseRemoteDataSource: SupabaseExpenseRemoteDataSource
    private lateinit var supabaseClient: SupabaseClient
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var pendingExpenseDeletionDao: PendingExpenseDeletionDao
    private lateinit var deletionTransaction: ExpenseDeletionTransaction
    private lateinit var repository: ExpenseRepositoryImpl

    private val expenseSyncEnqueuer =
        mockk<ExpenseSyncEnqueuer>(relaxed = true)

    private val remoteDataSync =
        mockk<RemoteDataSync>(relaxed = true)

    @Before
    fun setup() {
        expenseDao = mockk()
        supabaseExpenseRemoteDataSource = mockk()
        supabaseClient = mockk()
        categoryRepository = mockk()
        pendingExpenseDeletionDao = mockk()
        deletionTransaction = mockk()

        repository = ExpenseRepositoryImpl(
            expenseDao = expenseDao,
            supabaseExpenseRemoteDataSource = supabaseExpenseRemoteDataSource,
            supabaseClient = supabaseClient,
            categoryRepository = categoryRepository,
            expenseSyncEnqueuer = expenseSyncEnqueuer,
            pendingExpenseDeletionDao = pendingExpenseDeletionDao,
            deletionTransaction = deletionTransaction,
            remoteDataSync = remoteDataSync
        )
    }

    @Test
    fun getExpensesReturnsMappedExpenses() = runTest {

        val entities = listOf(
            ExpenseEntity(
                id = 1L,
                amount = 100.50,
                categoryId = 1L,
                description = "Lunch",
                date = 1000L
            )
        )

        every {
            expenseDao.getExpenses()
        } returns flowOf(entities)


        val result = repository.getExpense()

        result.collect { expenses ->
            assertEquals(
                listOf(
                    Expense(
                        id = 1L,
                        amount = 100.50,
                        categoryId = 1L,
                        description = "Lunch",
                        date = 1000L
                    )
                ),
                expenses
            )
        }

    }

    @Test
    fun getExpenseByIdReturnsMappedExpense() = runTest {

        val entity = ExpenseEntity(
            id = 1L,
            amount = 100.50,
            categoryId = 1L,
            description = "Lunch",
            date = 1000L
        )

        coEvery {
            expenseDao.getExpenseById(1L)
        } returns entity

        val result = repository.getExpenseById(1L)

        assertEquals(
            Expense(
                1L,
                amount = 100.50,
                categoryId = 1L,
                description = "Lunch",
                date = 1000L
            ),
            result
        )

    }

    @Test
    fun addExpenseCallDaoWithMappedEntity() = runTest {
        val expense = Expense(
            1L,
            amount = 100.50,
            categoryId = 1L,
            description = "Lunch",
            date = 1000L
        )

        coEvery { expenseDao.insertExpense(any()) } returns Unit

        repository.addExpense(expense)

        coVerify(exactly = 1) {
            expenseDao.insertExpense(
                match { entity ->
                    entity.id == 1L &&
                            entity.amount == 100.50 &&
                            entity.categoryId == 1L &&
                            entity.description == "Lunch" &&
                            entity.date == 1000L &&
                            entity.syncStatus == SyncStatus.PENDING &&
                            entity.clientId.isNotBlank()
                }
            )
        }
    }

    @Test
    fun updateExpenseCallsDaoWithMappedEntity() = runTest {
        val existingEntity = ExpenseEntity(
            id = 1L,
            amount = 80.00,
            categoryId = 1L,
            description = "Breakfast",
            date = 900L,
            clientId = "test-client-id"
        )

        val expense = Expense(
            id = 1L,
            amount = 100.50,
            categoryId = 1L,
            description = "Lunch",
            date = 1000L
        )

        coEvery { expenseDao.getExpenseById(1L) } returns existingEntity
        coEvery { expenseDao.updateExpense(any()) } returns Unit

        repository.updateExpense(expense)

        coVerify(exactly = 1) {
            expenseDao.updateExpense(
                match { entity ->
                    entity.id == 1L &&
                            entity.amount == 100.50 &&
                            entity.categoryId == 1L &&
                            entity.description == "Lunch" &&
                            entity.date == 1000L &&
                            entity.clientId == "test-client-id" &&
                            entity.syncStatus == SyncStatus.PENDING
                }
            )
        }
    }

    @Test
    fun deleteExpenseCallsDeletionTransaction() = runTest {
        val expenseEntity = ExpenseEntity(
            id = 1L,
            amount = 100.50,
            categoryId = 1L,
            description = "Lunch",
            date = 1000L,
            clientId = "test-client-id"
        )

        val expense = Expense(
            id = 1L,
            amount = 100.50,
            categoryId = 1L,
            description = "Lunch",
            date = 1000L
        )

        coEvery { expenseDao.getExpenseById(1L) } returns expenseEntity
        coEvery {
            deletionTransaction.deleteExpense(1L, "test-client-id")
        } returns Unit

        repository.deleteExpense(expense)

        coVerify(exactly = 1) {
            expenseDao.getExpenseById(1L)
        }

        coVerify(exactly = 1) {
            deletionTransaction.deleteExpense(1L, "test-client-id")
        }
    }



}