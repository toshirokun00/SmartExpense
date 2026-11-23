package com.smartexpense.di

import com.smartexpense.StartupViewModel
import com.smartexpense.domain.network.NetworkStatusProvider
import com.smartexpense.network.AndroidNetworkStatusProvider
import org.koin.android.ext.koin.androidContext
import com.smartexpense.viewmodel.AppThemeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val mainModule  = module {
    single<AndroidNetworkStatusProvider> { AndroidNetworkStatusProvider(androidContext()) }
    single<NetworkStatusProvider> { get<AndroidNetworkStatusProvider>() }

    viewModel {
        AppThemeViewModel(get())

    }
    viewModel {
        StartupViewModel(get(), get())
    }
}