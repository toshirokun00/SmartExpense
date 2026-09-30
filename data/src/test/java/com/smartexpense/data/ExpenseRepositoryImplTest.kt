package com.smartexpense.data

import com.smartexpense.data.repositoryimpl.ExpenseRepositoryImpl
import com.smartexpense.database.dao.ExpenseDao
import com.smartexpense.database.entity.ExpenseEntity
import com.smartexpense.domain.model.Expense
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
    private lateinit var repository: ExpenseRepositoryImpl

    @Before
    fun setup() {
        expenseDao = mockk()
        repository = ExpenseRepositoryImpl(expenseDao)
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
        coEvery {
            expenseDao.insertExpense(any())
        } returns Unit

        repository.addExpense(expense)

        coVerify {
            expenseDao.insertExpense(
                ExpenseEntity(
                    1L,
                    amount = 100.50,
                    categoryId = 1L,
                    description = "Lunch",
                    date = 1000L
                )
            )
        }
    }

    @Test
    fun updateExpenseCallsDaoWithMappedEntity() = runTest {
        val expense = Expense(
            1L,
            amount = 100.50,
            categoryId = 1L,
            description = "Lunch",
            date = 1000L
        )

        coEvery {
            expenseDao.updateExpense(any())
        } returns Unit

        repository.updateExpense(expense)

        coVerify {
            expenseDao.updateExpense(
                ExpenseEntity(
                    1L,
                    amount = 100.50,
                    categoryId = 1L,
                    description = "Lunch",
                    date = 1000L
                )
            )

        }
    }

    @Test
    fun deleteExpenseCallsDaoWithMappedEntity() = runTest {
        val expense = Expense(
            1L,
            amount = 100.50,
            categoryId = 1L,
            description = "Lunch",
            date = 1000L
        )

        coEvery {
            expenseDao.deleteExpense(any())
        } returns Unit

        repository.deleteExpense(expense)

        coVerify {
            expenseDao.deleteExpense(
                expense = ExpenseEntity(
                    1L,
                    amount = 100.50,
                    categoryId = 1L,
                    description = "Lunch",
                    date = 1000L
                )
            )
        }


    }


}