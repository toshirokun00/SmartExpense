package com.smartexpense.data.remote
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlin.coroutines.cancellation.CancellationException

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
        return try {
            selectExpenses(userId = userId, includeMerchant = true, includeExtended = true)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            try {
                selectExpenses(userId = userId, includeMerchant = true, includeExtended = false)
            } catch (ex: Exception) {
                selectExpenses(userId = userId, includeMerchant = false, includeExtended = false)
            }
        }
    }

    private suspend fun selectExpenses(
        userId: String,
        includeMerchant: Boolean,
        includeExtended: Boolean
    ): List<SupabaseExpenseResponseDto> {
        val columns = buildList {
            add("id")
            add("client_id")
            add("user_id")
            add("amount")
            add("category_id")
            add("description")
            if (includeMerchant) add("merchant")
            if (includeExtended) {
                add("payment_method")
                add("expense_type")
                add("tags")
                add("notes")
            }
            add("expense_date")
        }

        return supabaseClient
            .from("expenses")
            .select(columns = Columns.list(*columns.toTypedArray())) {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList<SupabaseExpenseResponseDto>()
    }

}
