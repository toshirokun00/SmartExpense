package com.smartexpense.domain.di

import com.smartexpense.domain.usecase.AddExpenseUseCase
import com.smartexpense.domain.usecase.BudgetUseCase
import com.smartexpense.domain.usecase.CategoryUseCase
import com.smartexpense.domain.usecase.DeleteExpenseUseCase
import com.smartexpense.domain.usecase.EvaluateBudgetAlertsUseCase
import com.smartexpense.domain.usecase.GetBudgetInsightsUseCase
import com.smartexpense.domain.usecase.GetExpenseByIdUseCase
import com.smartexpense.domain.usecase.GetExpenseReportUseCase
import com.smartexpense.domain.usecase.GetExpenseUseCase
import com.smartexpense.domain.usecase.ProcessRecurringExpensesUseCase
import com.smartexpense.domain.usecase.RecurringExpenseUseCase
import com.smartexpense.domain.usecase.SearchExpensesUseCase
import com.smartexpense.domain.usecase.SettingsUseCase
import com.smartexpense.domain.usecase.ScheduleSyncUseCase
import com.smartexpense.domain.usecase.UpdateExpenseUseCase
import com.smartexpense.domain.usecase.ValidateAmountUseCase
import org.koin.dsl.module
import java.time.Clock

val domainModule = module {

    single<Clock> {
        Clock.systemDefaultZone()
    }

    factory { GetExpenseUseCase(get()) }
    factory { GetExpenseByIdUseCase(get()) }
    factory { AddExpenseUseCase(get()) }
    factory { UpdateExpenseUseCase(get()) }
    factory { DeleteExpenseUseCase(get()) }
    factory { ValidateAmountUseCase() }

    factory { CategoryUseCase(get()) }

    factory { BudgetUseCase(get()) }

    factory { GetBudgetInsightsUseCase() }

    factory { EvaluateBudgetAlertsUseCase() }

    factory { SettingsUseCase(get()) }

    factory { GetExpenseReportUseCase(expenseRepository = get()) }

    factory { SearchExpensesUseCase(get()) }

    factory { ScheduleSyncUseCase(syncEnqueuer = get()) }

    factory { RecurringExpenseUseCase(repository = get()) }

    single { ProcessRecurringExpensesUseCase(repository = get()) }
}