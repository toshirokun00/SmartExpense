package com.smartexpense.data.remote
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

class SupabaseExpenseRemoteDataSource(
    private val supabaseClient: SupabaseClient
) {
    suspend fun insertExpense(expense: SupabaseExpenseDto) {
        supabaseClient
            .from("expenses")
            .upsert(expense) {
                onConflict = "client_id"
            }
    }

    suspend fun deleteExpenseByClientId(clientId: String) {
        supabaseClient
            .from("expenses")
            .delete {
                filter {
                    eq("client_id", clientId)
                }
            }
    }

}