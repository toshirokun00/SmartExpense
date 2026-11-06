package com.smartexpense.data.remote

import android.net.Uri
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * Persists profile data (username + avatar URL) to Supabase so it survives
 * device reinstalls.
 *
 * - Username: stored in Supabase Auth `userMetadata` (a JSON column on auth.users)
 * - Avatar: uploaded to Supabase Storage `avatars` bucket; the public URL is
 *   also written to `userMetadata` under the `avatar` key
 */
class SupabaseProfileRemoteDataSource(
    private val supabaseClient: SupabaseClient,
    private val storageDataSource: SupabaseStorageRemoteDataSource
) {

    /**
     * Saves the user's username and/or avatar to Supabase. If [avatarUri]
     * is a `content://` URI it is first uploaded to Supabase Storage.
     * Set [deleteAvatar] = true to clear a previously uploaded avatar.
     */
    suspend fun saveProfile(
        username: String?,
        avatarUri: String?,
        deleteAvatar: Boolean = false
    ) {
        val user = supabaseClient.auth.currentUserOrNull()
        val userId = user?.id ?: return

        var resolvedAvatar: String? = avatarUri
        if (deleteAvatar) {
            storageDataSource.deleteAvatarPhoto(userId)
            resolvedAvatar = null
        } else if (avatarUri != null && avatarUri.startsWith("content://")) {
            val uploadedUrl = storageDataSource.uploadAvatarPhoto(
                sourceUri = Uri.parse(avatarUri),
                userId = userId
            )
            if (uploadedUrl != null) resolvedAvatar = uploadedUrl
        }

        try {
            val updatedMetadata = buildJsonObject {
                // Username + avatar always win. Other keys (if any) are
                // intentionally not preserved since this app only stores
                // those two fields in userMetadata.
                username?.let { put("username", JsonPrimitive(it)) }
                if (!deleteAvatar) {
                    resolvedAvatar?.let { put("avatar", JsonPrimitive(it)) }
                }
            }

            supabaseClient.auth.updateUser {
                this.data = updatedMetadata
            }
        } catch (_: Exception) {
            // Offline or transient — caller's local Room copy is still saved.
        }
    }

    /**
     * Fetches the user's username + avatar URL from Supabase user metadata.
     */
    suspend fun loadProfile(): ProfileData? {
        return try {
            val user = supabaseClient.auth.retrieveUserForCurrentSession()
            val metadata = user.userMetadata
            val username = metadata?.get("username")?.jsonPrimitiveContentOrNull()
            val avatar = metadata?.get("avatar")?.jsonPrimitiveContentOrNull()
            val result = ProfileData(username, avatar)
            if (result.username == null && result.avatarUri == null) null else result
        } catch (_: Exception) {
            null
        }
    }

    data class ProfileData(
        val username: String?,
        val avatarUri: String?
    )

    private fun kotlinx.serialization.json.JsonElement.jsonPrimitiveContentOrNull(): String? =
        (this as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull
}
