package com.smartexpense.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

class SupabaseCategoryRemoteDataSource(
    private val supabaseClient: SupabaseClient
) {
    suspend fun upsertCategory(
        category: SupabaseCategoryDto
    ): SupabaseCategoryResponseDto {
        supabaseClient
            .from("categories")
            .upsert(category) {
                onConflict = "client_id"
            }

        return getCategoryByClientId(category.clientId)
            ?: error(
                "Category not found in Supabase after upsert: ${category.clientId}"
            )
    }
    suspend fun getCategoryByClientId(
        clientId: String
    ): SupabaseCategoryResponseDto? {
        return supabaseClient
            .from("categories")
            .select(
                columns = Columns.list("id", "client_id", "name")
            ) {
                filter {
                    eq("client_id", clientId)
                }
            }
            .decodeList<SupabaseCategoryResponseDto>()
            .firstOrNull()
    }

    suspend fun getCategories(
        userId: String
    ): List<SupabaseCategoryResponseDto> {
        return supabaseClient
            .from("categories")
            .select(
                columns = Columns.list("id", "client_id", "name")
            ) {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList<SupabaseCategoryResponseDto>()
    }
}