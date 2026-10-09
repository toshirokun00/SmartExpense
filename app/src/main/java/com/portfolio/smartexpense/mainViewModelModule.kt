package com.portfolio.smartexpense

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val mainModule  = module {
    viewModel {
        AppThemeViewModel(get())

    }
}