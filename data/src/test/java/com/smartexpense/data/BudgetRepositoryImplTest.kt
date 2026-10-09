package com.smartexpense.data

import com.smartexpense.data.repositoryimpl.BudgetRepositoryImpl
import com.smartexpense.database.dao.BudgetDao
import com.smartexpense.domain.model.Budget
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class BudgetRepositoryImplTest {

    private lateinit var budgetDao: BudgetDao

    private lateinit var budgetRepository: BudgetRepositoryImpl

    @Before
    fun setup() {
        budgetDao = mockk()
        budgetRepository = BudgetRepositoryImpl(budgetDao)
    }

    @Test
    fun addBudgetCallsDao() = runTest {

        val budget = Budget(
            id = 1L,
            categoryId = 1L,
            amount = 5000.0,
            startDate = 1L,
            endDate = 30L
        )

        coEvery {
            budgetDao.insertBudget(any())
        } just Runs

        budgetRepository.addBudget(budget)

        coVerify {
            budgetDao.insertBudget(
                match {
                    it.categoryId == 1L &&
                            it.amount == 5000.0 &&
                            it.startDate == 1L &&
                            it.endDate == 30L
                }
            )
        }
    }

}