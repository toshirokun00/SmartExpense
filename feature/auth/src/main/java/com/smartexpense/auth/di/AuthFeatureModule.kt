package com.smartexpense.auth.di

import com.smartexpense.auth.viewmodel.AuthViewModel
import com.smartexpense.domain.usecase.AuthUseCase
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authFeatureModule = module {
    single { AuthUseCase(get(), get()) }
    viewModel { AuthViewModel(get(), get()) }
}