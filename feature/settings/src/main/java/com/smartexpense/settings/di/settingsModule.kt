package com.smartexpense.settings.di

import com.smartexpense.settings.viewmodel.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module


val settingsModule = module {

    viewModel {
        SettingsViewModel(
            settingsUseCase = get(),
            authUseCase = get()
        )
    }
}