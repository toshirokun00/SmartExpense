package com.smartexpense.data.di

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import org.koin.dsl.module
import com.smartexpense.data.BuildConfig
import com.smartexpense.data.auth.SupabaseAuthRepository
import com.smartexpense.domain.repository.AuthRepository


val supabaseModule = module {
    single {
        check(BuildConfig.SUPABASE_URL.isNotBlank()) {
            "SUPABASE_URL is not configured"
        }

        check(BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()) {
            "SUPABASE_PUBLISHABLE_KEY is not configured"
        }

        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
        ) {
            install(Auth)
        }
    }

    single<AuthRepository> {
        SupabaseAuthRepository(
            supabaseClient = get()
        )
    }
}
