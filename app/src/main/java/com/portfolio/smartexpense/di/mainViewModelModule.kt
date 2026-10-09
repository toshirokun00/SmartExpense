package com.portfolio.smartexpense.di

import com.portfolio.smartexpense.viewmodel.AppThemeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val mainModule  = module {
    viewModel {
        AppThemeViewModel(get())

    }
}