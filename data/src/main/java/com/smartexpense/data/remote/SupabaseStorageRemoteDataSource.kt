package com.smartexpense.data.remote

import android.content.Context
import android.net.Uri
import com.smartexpense.data.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Uploads receipt photo bytes to Supabase Storage bucket `receipts` and returns
 * the public `https://` URL that survives app reinstall / new device login.
 */
class SupabaseStorageRemoteDataSource(
    private val supabaseClient: SupabaseClient,
    private val context: Context
) {

    suspend fun uploadReceiptPhoto(
        sourceUri: Uri,
        fileName: String = "receipt_${System.currentTimeMillis()}.jpg"
    ): String? {
        return try {
            val bytes = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                ByteArrayOutputStream().apply { input.copyTo(this) }.toByteArray()
            } ?: return null

            val baseUrl = BuildConfig.SUPABASE_URL.removeSuffix("/")
            val project = baseUrl.removePrefix("https://").split(".").firstOrNull()
                ?: return null
            val token = supabaseClient.auth.currentAccessTokenOrNull() ?: return null

            val uploadUrl = URL("$baseUrl/storage/v1/object/receipts/$fileName")
            val conn = uploadUrl.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "image/jpeg")
            conn.setRequestProperty("x-upsert", "true")
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.outputStream.use { it.write(bytes) }

            if (conn.responseCode in 200..299) {
                "$baseUrl/storage/v1/object/public/receipts/$fileName"
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
