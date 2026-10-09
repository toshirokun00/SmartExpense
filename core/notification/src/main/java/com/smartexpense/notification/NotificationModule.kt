package com.smartexpense.notification

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val notificationModule = module {
    single {
        ExpenseNotificationManager(
            context = androidContext()
        )
    }
}