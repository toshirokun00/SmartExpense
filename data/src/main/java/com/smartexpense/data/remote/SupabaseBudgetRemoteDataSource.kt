package com.smartexpense.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

class SupabaseBudgetRemoteDataSource(
    private val supabaseClient: SupabaseClient
) {
    suspend fun upsertBudget(budget: SupabaseBudgetDto) {
        supabaseClient
            .from("budgets")
            .upsert(budget) {
                onConflict = "client_id"
            }
    }

    suspend fun deleteBudgetByClientId(clientId: String) {
        supabaseClient
            .from("budgets")
            .delete {
                filter {
                    eq("client_id", clientId)
                }
            }
    }

    suspend fun getBudgets(userId: String): List<SupabaseBudgetResponseDto> =
        supabaseClient
            .from("budgets")
            .select(
                columns = Columns.list(
                    "id",
                    "client_id",
                    "user_id",
                    "category_id",
                    "amount",
                    "start_date",
                    "end_date"
                )
            ) {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList()
}
