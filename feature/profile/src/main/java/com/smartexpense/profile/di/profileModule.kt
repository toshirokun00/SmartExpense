package com.smartexpense.profile.di

import com.smartexpense.profile.viewmodel.ProfileViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val profileModule = module {

    viewModel {
        ProfileViewModel(
            profileUseCase = get(),
            authUseCase = get()
        )
    }
}
