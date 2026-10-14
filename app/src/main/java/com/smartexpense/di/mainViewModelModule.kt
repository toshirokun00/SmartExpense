package com.smartexpense.di

import com.smartexpense.StartupViewModel
import com.smartexpense.viewmodel.AppThemeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val mainModule  = module {
    viewModel {
        AppThemeViewModel(get())

    }
    viewModel {
        StartupViewModel(get())
    }
}