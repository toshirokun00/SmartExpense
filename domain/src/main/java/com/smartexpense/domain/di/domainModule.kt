package com.smartexpense.domain.di

import com.smartexpense.domain.usecase.AddExpenseUseCase
import com.smartexpense.domain.usecase.BudgetUseCase
import com.smartexpense.domain.usecase.CategoryUseCase
import com.smartexpense.domain.usecase.DeleteExpenseUseCase
import com.smartexpense.domain.usecase.GetExpenseByIdUseCase
import com.smartexpense.domain.usecase.GetExpenseUseCase
import com.smartexpense.domain.usecase.UpdateExpenseUseCase
import com.smartexpense.domain.usecase.ValidateAmountUseCase
import org.koin.dsl.module

val domainModule = module {

    factory { GetExpenseUseCase(get()) }
    factory { GetExpenseByIdUseCase(get()) }
    factory { AddExpenseUseCase(get()) }
    factory { UpdateExpenseUseCase(get()) }
    factory { DeleteExpenseUseCase(get()) }
    factory { ValidateAmountUseCase() }

    factory { CategoryUseCase(get()) }

    factory { BudgetUseCase(get()) }

}