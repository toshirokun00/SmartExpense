package com.smartexpense

import android.app.Application
import com.smartexpense.budget.di.budgetModule
import com.smartexpense.reports.di.expenseReportModule
import com.smartexpense.settings.di.settingsModule
import com.smartexpense.di.mainModule
import com.smartexpene.categories.di.categoryModule
import com.smartexpense.auth.di.authFeatureModule
import com.smartexpense.core.common.dataStoreModule
import com.smartexpense.dashoard.di.dashboardModule
import com.smartexpense.data.di.dataModule
import com.smartexpense.data.di.supabaseModule
import com.smartexpense.database.di.databaseModule
import com.smartexpense.domain.di.domainModule
import com.smartexpense.feature.expenses.di.expenseModule
import com.smartexpense.notification.notificationModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SmartExpenseApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@SmartExpenseApplication)

            modules(
                mainModule,
                databaseModule,
                dataModule,
                domainModule,
                expenseModule,
                categoryModule,
                dashboardModule,
                budgetModule,
                settingsModule,
                dataStoreModule,
                notificationModule,
                expenseReportModule,
                supabaseModule,
                authFeatureModule
            )
        }
    }
}