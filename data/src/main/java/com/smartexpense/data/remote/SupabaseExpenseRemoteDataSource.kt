package com.smartexpense.data.remote
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

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

    suspend fun getExpenses(
        userId: String
    ): List<SupabaseExpenseResponseDto> {
        return supabaseClient
            .from("expenses")
            .select(
                columns = Columns.list(
                    "id",
                    "client_id",
                    "user_id",
                    "amount",
                    "category_id",
                    "description",
                    "expense_date"
                )
            ) {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList<SupabaseExpenseResponseDto>()
    }

}