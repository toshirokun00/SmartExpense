package com.smartexpense.data.remote

import android.content.Context
import android.net.Uri
import com.smartexpense.data.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Uploads receipt photo bytes to Supabase Storage bucket `receipts` and returns
 * the public `https://` URL that survives app reinstall / new device login.
 */
class SupabaseStorageRemoteDataSource(
    private val supabaseClient: SupabaseClient,
    private val context: Context
) {

    /**
     * Uploads the picked avatar to the public `avatars` bucket and returns its
     * public URL.
     *
     * Runs on [Dispatchers.IO] — the caller (a ViewModel coroutine) is on the
     * main thread, and `HttpURLConnection` throws
     * `NetworkOnMainThreadException` otherwise.
     *
     * @throws IOException when the image cannot be read or Supabase rejects
     * the upload, so callers can surface the real reason instead of silently
     * falling back to a `content://` URI.
     */
    suspend fun uploadAvatarPhoto(
        sourceUri: Uri,
        userId: String
    ): String = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(sourceUri)?.use { input ->
            ByteArrayOutputStream().apply { input.copyTo(this) }.toByteArray()
        } ?: throw IOException("Could not read the selected image")

        val baseUrl = BuildConfig.SUPABASE_URL.removeSuffix("/")
        val token = supabaseClient.auth.currentAccessTokenOrNull()
            ?: throw IOException("Your session has expired. Please sign in again.")

        // Keep the fixed file name for simple overwrite/delete, but send the
        // real MIME type so Supabase stores a decodable image.
        val mimeType = context.contentResolver.getType(sourceUri) ?: "image/jpeg"
        val fileName = "avatar_$userId.jpg"

        val uploadUrl = URL("$baseUrl/storage/v1/object/avatars/$fileName")
        val conn = uploadUrl.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
        conn.setRequestProperty("Content-Type", mimeType)
        conn.setRequestProperty("x-upsert", "true")
        conn.connectTimeout = 15_000
        conn.readTimeout = 15_000
        conn.outputStream.use { it.write(bytes) }

        val responseCode = conn.responseCode
        if (responseCode !in 200..299) {
            val message = conn.errorStream
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
                .take(200)
            throw IOException(
                "Avatar upload failed (HTTP $responseCode)" +
                    if (message.isBlank()) "" else ": $message"
            )
        }

        "$baseUrl/storage/v1/object/public/avatars/$fileName"
    }

    suspend fun deleteAvatarPhoto(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val baseUrl = BuildConfig.SUPABASE_URL.removeSuffix("/")
            val token = supabaseClient.auth.currentAccessTokenOrNull() ?: return@withContext false
            val fileName = "avatar_$userId.jpg"
            val deleteUrl = URL("$baseUrl/storage/v1/object/avatars/$fileName")
            val conn = deleteUrl.openConnection() as HttpURLConnection
            conn.requestMethod = "DELETE"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.responseCode in 200..299
        } catch (e: Exception) {
            false
        }
    }

    suspend fun uploadReceiptPhoto(
        sourceUri: Uri,
        fileName: String = "receipt_${System.currentTimeMillis()}.jpg"
    ): String? = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                ByteArrayOutputStream().apply { input.copyTo(this) }.toByteArray()
            } ?: return@withContext null

            val baseUrl = BuildConfig.SUPABASE_URL.removeSuffix("/")
            val token = supabaseClient.auth.currentAccessTokenOrNull() ?: return@withContext null

            val uploadUrl = URL("$baseUrl/storage/v1/object/receipts/$fileName")
            val conn = uploadUrl.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
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
