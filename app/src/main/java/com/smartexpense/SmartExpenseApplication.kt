package com.smartexpense

import android.app.Application
import androidx.work.Configuration
import com.smartexpene.categories.di.categoryModule
import com.smartexpense.auth.di.authFeatureModule
import com.smartexpense.budget.alerts.BudgetAlertCoordinator
import com.smartexpense.budget.di.budgetModule
import com.smartexpense.core.common.dataStoreModule
import com.smartexpense.dashoard.di.dashboardModule
import com.smartexpense.data.di.dataModule
import com.smartexpense.data.di.supabaseModule
import com.smartexpense.data.sync.ExpenseSyncWorkerFactory
import com.smartexpense.database.di.databaseModule
import com.smartexpense.di.mainModule
import com.smartexpense.domain.di.domainModule
import com.smartexpense.feature.expenses.di.expenseModule
import com.smartexpense.notification.notificationModule
import com.smartexpense.reports.di.expenseReportModule
import com.smartexpense.settings.di.settingsModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin

class SmartExpenseApplication : Application(), Configuration.Provider, KoinComponent {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(org.koin.core.logger.Level.ERROR)
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

        // Observe budgets/expenses so alerts fire when a threshold is crossed.
        get<BudgetAlertCoordinator>().start(applicationScope)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(get<ExpenseSyncWorkerFactory>())
            .build()
}
