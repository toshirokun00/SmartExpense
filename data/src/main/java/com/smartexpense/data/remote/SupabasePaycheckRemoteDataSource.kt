package com.smartexpense.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

class SupabasePaycheckRemoteDataSource(
    private val supabaseClient: SupabaseClient
) {
    suspend fun upsertPaycheck(paycheck: SupabasePaycheckDto) {
        supabaseClient
            .from("paychecks")
            .upsert(paycheck) {
                onConflict = "client_id"
            }
    }

    suspend fun deletePaycheckByClientId(clientId: String) {
        supabaseClient
            .from("paychecks")
            .delete {
                filter {
                    eq("client_id", clientId)
                }
            }
    }

    suspend fun getPaychecks(userId: String): List<SupabasePaycheckResponseDto> =
        supabaseClient
            .from("paychecks")
            .select(
                columns = Columns.list(
                    "id",
                    "client_id",
                    "user_id",
                    "amount",
                    "pay_date",
                    "label",
                    "created_at",
                    "schedule_client_id",
                    "occurrence_date",
                    "is_skipped"
                )
            ) {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList()

    /** Clears the remote assignments before re-uploading a paycheck's selection. */
    suspend fun deletePaycheckBills(paycheckClientId: String) {
        supabaseClient
            .from("paycheck_bills")
            .delete {
                filter {
                    eq("paycheck_client_id", paycheckClientId)
                }
            }
    }

    suspend fun upsertPaycheckBill(assignment: SupabasePaycheckBillDto) {
        supabaseClient
            .from("paycheck_bills")
            .upsert(assignment) {
                onConflict = "client_id"
            }
    }

    suspend fun getPaycheckBills(userId: String): List<SupabasePaycheckBillResponseDto> =
        supabaseClient
            .from("paycheck_bills")
            .select(
                columns = Columns.list(
                    "id",
                    "client_id",
                    "user_id",
                    "paycheck_client_id",
                    "bill_client_id",
                    "created_at"
                )
            ) {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList()

    suspend fun upsertPaycheckSchedule(schedule: SupabasePaycheckScheduleDto) {
        supabaseClient
            .from("paycheck_schedules")
            .upsert(schedule) {
                onConflict = "client_id"
            }
    }

    suspend fun getPaycheckSchedules(userId: String): List<SupabasePaycheckScheduleResponseDto> =
        supabaseClient
            .from("paycheck_schedules")
            .select(
                columns = Columns.list(
                    "id",
                    "client_id",
                    "user_id",
                    "amount",
                    "label",
                    "frequency",
                    "first_day",
                    "second_day",
                    "start_date",
                    "end_date",
                    "is_active",
                    "created_at"
                )
            ) {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList()
}
