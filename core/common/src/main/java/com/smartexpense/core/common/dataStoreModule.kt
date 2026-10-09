package com.smartexpense.core.common

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataStoreModule = module {

    single {
        SettingsDataStore(
            context = androidContext()
        )
    }
}