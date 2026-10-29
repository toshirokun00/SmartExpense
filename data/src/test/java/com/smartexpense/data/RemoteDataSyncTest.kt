package com.smartexpense.data

import com.smartexpense.data.remote.SupabaseBudgetRemoteDataSource
import com.smartexpense.data.remote.SupabaseBudgetResponseDto
import com.smartexpense.data.remote.SupabaseCategoryRemoteDataSource
import com.smartexpense.data.remote.SupabaseCategoryResponseDto
import com.smartexpense.data.remote.SupabaseExpenseRemoteDataSource
import com.smartexpense.data.remote.SupabaseExpenseResponseDto
import com.smartexpense.data.sync.RemoteDataSync
import com.smartexpense.database.dao.BudgetDao
import com.smartexpense.database.dao.CategoryDao
import com.smartexpense.database.dao.ExpenseDao
import com.smartexpense.database.dao.PendingBudgetDeletionDao
import com.smartexpense.database.dao.PendingExpenseDeletionDao
import com.smartexpense.database.entity.BudgetEntity
import com.smartexpense.database.entity.CategoryEntity
import com.smartexpense.database.entity.PendingBudgetDeletionEntity
import com.smartexpense.database.entity.PendingExpenseDeletionEntity
import com.smartexpense.database.entity.SyncStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RemoteDataSyncTest {

    private lateinit var categoryDao: CategoryDao
    private lateinit var expenseDao: ExpenseDao
    private lateinit var budgetDao: BudgetDao
    private lateinit var pendingExpenseDeletionDao: PendingExpenseDeletionDao
    private lateinit var pendingBudgetDeletionDao: PendingBudgetDeletionDao
    private lateinit var categoryRemoteDataSource: SupabaseCategoryRemoteDataSource
    private lateinit var expenseRemoteDataSource: SupabaseExpenseRemoteDataSource
    private lateinit var budgetRemoteDataSource: SupabaseBudgetRemoteDataSource

    private lateinit var remoteDataSync: RemoteDataSync

    @Before
    fun setUp() {
        categoryDao = mockk(relaxed = true)
        expenseDao = mockk(relaxed = true)
        budgetDao = mockk(relaxed = true)
        pendingExpenseDeletionDao = mockk(relaxed = true)
        pendingBudgetDeletionDao = mockk(relaxed = true)
        categoryRemoteDataSource = mockk(relaxed = true)
        expenseRemoteDataSource = mockk(relaxed = true)
        budgetRemoteDataSource = mockk(relaxed = true)

        remoteDataSync = RemoteDataSync(
            categoryDao = categoryDao,
            expenseDao = expenseDao,
            budgetDao = budgetDao,
            pendingExpenseDeletionDao = pendingExpenseDeletionDao,
            pendingBudgetDeletionDao = pendingBudgetDeletionDao,
            categoryRemoteDataSource = categoryRemoteDataSource,
            expenseRemoteDataSource = expenseRemoteDataSource,
            budgetRemoteDataSource = budgetRemoteDataSource
        )
    }

    @Test
    fun `restores remote categories that are missing locally`() = runTest {
        coEvery {
            categoryRemoteDataSource.getCategories(USER_ID)
        } returns listOf(
            SupabaseCategoryResponseDto(
                id = 10L,
                clientId = "cat-a",
                name = "Food"
            )
        )

        coEvery { categoryDao.getCategoriesOnce() } returnsMany listOf(
            emptyList(),
            listOf(
                CategoryEntity(id = 1L, name = "Food", clientId = "cat-a")
            )
        )

        coEvery { expenseRemoteDataSource.getExpenses(USER_ID) } returns emptyList()

        remoteDataSync.pullRemoteData(USER_ID)

        coVerify(exactly = 1) {
            categoryDao.insertCategory(
                match { it.clientId == "cat-a" && it.name == "Food" }
            )
        }
    }

    @Test
    fun `does not duplicate categories already present locally`() = runTest {
        coEvery {
            categoryRemoteDataSource.getCategories(USER_ID)
        } returns listOf(
            SupabaseCategoryResponseDto(
                id = 10L,
                clientId = "cat-a",
                name = "Food"
            )
        )

        coEvery { categoryDao.getCategoriesOnce() } returns listOf(
            CategoryEntity(id = 1L, name = "Food", clientId = "cat-a")
        )

        coEvery { expenseRemoteDataSource.getExpenses(USER_ID) } returns emptyList()

        remoteDataSync.pullRemoteData(USER_ID)

        coVerify(exactly = 0) {
            categoryDao.insertCategory(any())
        }
    }

    @Test
    fun `restores remote expenses as synced and maps the local category id`() =
        runTest {
            coEvery {
                categoryRemoteDataSource.getCategories(USER_ID)
            } returns listOf(
                SupabaseCategoryResponseDto(
                    id = 10L,
                    clientId = "cat-a",
                    name = "Food"
                )
            )

            coEvery { categoryDao.getCategoriesOnce() } returnsMany listOf(
                emptyList(),
                listOf(
                    CategoryEntity(id = 5L, name = "Food", clientId = "cat-a")
                )
            )

            coEvery {
                expenseRemoteDataSource.getExpenses(USER_ID)
            } returns listOf(
                SupabaseExpenseResponseDto(
                    id = 1L,
                    clientId = "exp-1",
                    userId = USER_ID,
                    amount = 50.0,
                    categoryId = 10L,
                    description = "Lunch",
                    expenseDate = "2026-10-16T07:43:21.334Z"
                )
            )

            coEvery { expenseDao.getAllClientIds() } returns emptyList()
            coEvery {
                pendingExpenseDeletionDao.getPendingDeletions()
            } returns emptyList()

            remoteDataSync.pullRemoteData(USER_ID)

            coVerify(exactly = 1) {
                expenseDao.insertExpense(
                    match {
                        it.clientId == "exp-1" &&
                            it.categoryId == 5L &&
                            it.amount == 50.0 &&
                            it.description == "Lunch" &&
                            it.syncStatus == SyncStatus.SYNCED
                    }
                )
            }
        }

    @Test
    fun `skips remote expenses that already exist locally`() = runTest {
        coEvery {
            categoryRemoteDataSource.getCategories(USER_ID)
        } returns listOf(
            SupabaseCategoryResponseDto(
                id = 10L,
                clientId = "cat-a",
                name = "Food"
            )
        )

        coEvery { categoryDao.getCategoriesOnce() } returns listOf(
            CategoryEntity(id = 5L, name = "Food", clientId = "cat-a")
        )

        coEvery {
            expenseRemoteDataSource.getExpenses(USER_ID)
        } returns listOf(remoteExpense(clientId = "exp-1"))

        coEvery { expenseDao.getAllClientIds() } returns listOf("exp-1")

        remoteDataSync.pullRemoteData(USER_ID)

        coVerify(exactly = 0) {
            expenseDao.insertExpense(any())
        }
    }

    @Test
    fun `skips remote expenses that have a queued local deletion`() = runTest {
        coEvery {
            categoryRemoteDataSource.getCategories(USER_ID)
        } returns listOf(
            SupabaseCategoryResponseDto(
                id = 10L,
                clientId = "cat-a",
                name = "Food"
            )
        )

        coEvery { categoryDao.getCategoriesOnce() } returns listOf(
            CategoryEntity(id = 5L, name = "Food", clientId = "cat-a")
        )

        coEvery {
            expenseRemoteDataSource.getExpenses(USER_ID)
        } returns listOf(remoteExpense(clientId = "exp-1"))

        coEvery { expenseDao.getAllClientIds() } returns emptyList()
        coEvery {
            pendingExpenseDeletionDao.getPendingDeletions()
        } returns listOf(PendingExpenseDeletionEntity(clientId = "exp-1"))

        remoteDataSync.pullRemoteData(USER_ID)

        coVerify(exactly = 0) {
            expenseDao.insertExpense(any())
        }
    }

    @Test
    fun `skips remote expenses whose category is unknown locally`() = runTest {
        coEvery {
            categoryRemoteDataSource.getCategories(USER_ID)
        } returns emptyList()

        coEvery {
            expenseRemoteDataSource.getExpenses(USER_ID)
        } returns listOf(remoteExpense(clientId = "exp-1", categoryId = 99L))

        coEvery { expenseDao.getAllClientIds() } returns emptyList()
        coEvery {
            pendingExpenseDeletionDao.getPendingDeletions()
        } returns emptyList()

        remoteDataSync.pullRemoteData(USER_ID)

        coVerify(exactly = 0) {
            expenseDao.insertExpense(any())
        }
    }

    @Test
    fun `skips remote expenses with an unparsable date`() = runTest {
        coEvery {
            categoryRemoteDataSource.getCategories(USER_ID)
        } returns listOf(
            SupabaseCategoryResponseDto(
                id = 10L,
                clientId = "cat-a",
                name = "Food"
            )
        )

        coEvery { categoryDao.getCategoriesOnce() } returns listOf(
            CategoryEntity(id = 5L, name = "Food", clientId = "cat-a")
        )

        coEvery {
            expenseRemoteDataSource.getExpenses(USER_ID)
        } returns listOf(
            remoteExpense(clientId = "exp-1", expenseDate = "not-a-date")
        )

        coEvery { expenseDao.getAllClientIds() } returns emptyList()
        coEvery {
            pendingExpenseDeletionDao.getPendingDeletions()
        } returns emptyList()

        remoteDataSync.pullRemoteData(USER_ID)

        coVerify(exactly = 0) {
            expenseDao.insertExpense(any())
        }
    }

    @Test
    fun `restores remote budgets as synced and maps the local category id`() =
        runTest {
            coEvery {
                categoryRemoteDataSource.getCategories(USER_ID)
            } returns listOf(
                SupabaseCategoryResponseDto(
                    id = 10L,
                    clientId = "cat-a",
                    name = "Food"
                )
            )

            coEvery { categoryDao.getCategoriesOnce() } returnsMany listOf(
                emptyList(),
                listOf(
                    CategoryEntity(id = 5L, name = "Food", clientId = "cat-a")
                )
            )

            coEvery {
                budgetRemoteDataSource.getBudgets(USER_ID)
            } returns listOf(
                SupabaseBudgetResponseDto(
                    id = 1L,
                    clientId = "budget-1",
                    userId = USER_ID,
                    categoryId = 10L,
                    amount = 1000.0,
                    startDate = "2026-10-01T00:00:00Z",
                    endDate = "2026-10-31T00:00:00Z"
                )
            )
            coEvery { budgetDao.getBudgetsOnce() } returns emptyList()
            coEvery { expenseRemoteDataSource.getExpenses(USER_ID) } returns emptyList()
            coEvery {
                pendingBudgetDeletionDao.getPendingDeletions()
            } returns emptyList()

            remoteDataSync.pullRemoteData(USER_ID)

            coVerify(exactly = 1) {
                budgetDao.insertBudgetIgnoringDuplicate(
                    match {
                        it.clientId == "budget-1" &&
                            it.categoryId == 5L &&
                            it.amount == 1000.0 &&
                            it.syncStatus == SyncStatus.SYNCED
                    }
                )
            }
        }

    @Test
    fun `skips remote budgets that already exist locally`() = runTest {
        coEvery {
            categoryRemoteDataSource.getCategories(USER_ID)
        } returns listOf(
            SupabaseCategoryResponseDto(
                id = 10L,
                clientId = "cat-a",
                name = "Food"
            )
        )
        coEvery { categoryDao.getCategoriesOnce() } returns listOf(
            CategoryEntity(id = 5L, name = "Food", clientId = "cat-a")
        )
        coEvery {
            budgetRemoteDataSource.getBudgets(USER_ID)
        } returns listOf(remoteBudget(clientId = "budget-1"))
        coEvery { budgetDao.getBudgetsOnce() } returns listOf(
            BudgetEntity(
                id = 1L,
                categoryId = 5L,
                amount = 1000.0,
                startDate = 1L,
                endDate = 30L,
                clientId = "budget-1"
            )
        )
        coEvery { expenseRemoteDataSource.getExpenses(USER_ID) } returns emptyList()

        remoteDataSync.pullRemoteData(USER_ID)

        coVerify(exactly = 0) {
            budgetDao.insertBudgetIgnoringDuplicate(any())
        }
    }

    @Test
    fun `skips remote budgets that have a queued local deletion`() = runTest {
        coEvery {
            categoryRemoteDataSource.getCategories(USER_ID)
        } returns listOf(
            SupabaseCategoryResponseDto(
                id = 10L,
                clientId = "cat-a",
                name = "Food"
            )
        )
        coEvery { categoryDao.getCategoriesOnce() } returns listOf(
            CategoryEntity(id = 5L, name = "Food", clientId = "cat-a")
        )
        coEvery {
            budgetRemoteDataSource.getBudgets(USER_ID)
        } returns listOf(remoteBudget(clientId = "budget-1"))
        coEvery { budgetDao.getBudgetsOnce() } returns emptyList()
        coEvery {
            pendingBudgetDeletionDao.getPendingDeletions()
        } returns listOf(PendingBudgetDeletionEntity(clientId = "budget-1"))
        coEvery { expenseRemoteDataSource.getExpenses(USER_ID) } returns emptyList()

        remoteDataSync.pullRemoteData(USER_ID)

        coVerify(exactly = 0) {
            budgetDao.insertBudgetIgnoringDuplicate(any())
        }
    }

    @Test
    fun `budget pull failure does not block expense restoration`() = runTest {
        coEvery {
            categoryRemoteDataSource.getCategories(USER_ID)
        } returns listOf(
            SupabaseCategoryResponseDto(
                id = 10L,
                clientId = "cat-a",
                name = "Food"
            )
        )
        coEvery { categoryDao.getCategoriesOnce() } returnsMany listOf(
            emptyList(),
            listOf(
                CategoryEntity(id = 5L, name = "Food", clientId = "cat-a")
            )
        )
        coEvery {
            expenseRemoteDataSource.getExpenses(USER_ID)
        } returns listOf(remoteExpense(clientId = "exp-1"))
        coEvery { expenseDao.getAllClientIds() } returns emptyList()
        coEvery {
            pendingExpenseDeletionDao.getPendingDeletions()
        } returns emptyList()
        coEvery {
            budgetRemoteDataSource.getBudgets(USER_ID)
        } throws IllegalStateException("budgets table missing")

        remoteDataSync.pullRemoteData(USER_ID)

        coVerify(exactly = 1) {
            expenseDao.insertExpense(
                match { it.clientId == "exp-1" }
            )
        }
    }

    private fun remoteBudget(clientId: String) = SupabaseBudgetResponseDto(
        id = 1L,
        clientId = clientId,
        userId = USER_ID,
        categoryId = 10L,
        amount = 1000.0,
        startDate = "2026-10-01T00:00:00Z",
        endDate = "2026-10-31T00:00:00Z"
    )

    private fun remoteExpense(
        clientId: String,
        categoryId: Long = 10L,
        expenseDate: String = "2026-10-16T07:43:21.334Z"
    ) = SupabaseExpenseResponseDto(
        id = 1L,
        clientId = clientId,
        userId = USER_ID,
        amount = 50.0,
        categoryId = categoryId,
        description = "Lunch",
        expenseDate = expenseDate
    )

    private companion object {
        const val USER_ID = "user-123"
    }
}
