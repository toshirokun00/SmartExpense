package com.smartexpense.domain

import com.smartexpense.domain.repository.BudgetRepository
import com.smartexpense.domain.usecase.BudgetUseCase
import io.mockk.mockk
import org.junit.Before

class BudgetUseTest {

    private lateinit var budgetRepository: BudgetRepository

    private lateinit var budgetUseCase: BudgetUseCase

    @Before
    fun setup() {
        budgetRepository  = mockk()
    }
}