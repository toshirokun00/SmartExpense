package com.smartexpense.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

class SupabaseBillRemoteDataSource(
    private val supabaseClient: SupabaseClient
) {
    suspend fun upsertBill(bill: SupabaseBillDto) {
        supabaseClient
            .from("bills")
            .upsert(bill) {
                onConflict = "client_id"
            }
    }

    suspend fun deleteBillByClientId(clientId: String) {
        supabaseClient
            .from("bills")
            .delete {
                filter {
                    eq("client_id", clientId)
                }
            }
    }

    suspend fun getBills(userId: String): List<SupabaseBillResponseDto> =
        supabaseClient
            .from("bills")
            .select(
                columns = Columns.list(
                    "id",
                    "client_id",
                    "user_id",
                    "name",
                    "amount",
                    "due_date",
                    "category_id",
                    "notes",
                    "frequency",
                    "repeat_interval",
                    "amount_type",
                    "amount_estimated",
                    "payee",
                    "payment_method",
                    "autopay",
                    "reminder_days_before",
                    "reminder_time_minutes",
                    "paid_at",
                    "skipped",
                    "is_active"
                )
            ) {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList()

    suspend fun upsertBillPayment(payment: SupabaseBillPaymentDto) {
        supabaseClient
            .from("bill_payments")
            .upsert(payment) {
                onConflict = "client_id"
            }
    }

    suspend fun getBillPayments(userId: String): List<SupabaseBillPaymentResponseDto> =
        supabaseClient
            .from("bill_payments")
            .select(
                columns = Columns.list(
                    "id",
                    "client_id",
                    "user_id",
                    "bill_client_id",
                    "bill_name",
                    "amount",
                    "paid_at",
                    "payment_method",
                    "created_at"
                )
            ) {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList()
}
