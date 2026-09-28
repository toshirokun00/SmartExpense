package com.portfolio.smartexpense

import android.app.Application
import com.smartexpense.data.di.dataModule
import com.smartexpense.database.di.databaseModule
import com.smartexpense.domain.di.domainModule
import com.smartexpense.feature.expenses.di.expenseModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SmartExpenseApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@SmartExpenseApplication)

            modules(
                databaseModule,
                dataModule,
                domainModule,
                expenseModule
            )
        }
    }
}