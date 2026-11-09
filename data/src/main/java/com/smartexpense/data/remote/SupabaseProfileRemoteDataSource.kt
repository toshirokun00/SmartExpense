package com.smartexpense.data.remote

import android.net.Uri
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.serialization.json.JsonNull
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
     *
     * @return the final avatar URI: the uploaded public URL when a new image
     * was uploaded, otherwise [avatarUri]. A `content://` URI is never written
     * to the remote metadata.
     * @throws java.io.IOException when the avatar upload fails, so the UI can
     * report it instead of silently keeping a device-only URI.
     */
    suspend fun saveProfile(
        username: String?,
        avatarUri: String?,
        deleteAvatar: Boolean = false
    ): String? {
        val user = supabaseClient.auth.currentUserOrNull()
        val userId = user?.id ?: return avatarUri

        var resolvedAvatar: String? = avatarUri
        if (deleteAvatar) {
            storageDataSource.deleteAvatarPhoto(userId)
            resolvedAvatar = null
        } else if (avatarUri != null && avatarUri.startsWith("content://")) {
            // Upload first. If this throws we abort before writing anything,
            // so metadata never receives an unusable content:// URI.
            resolvedAvatar = storageDataSource.uploadAvatarPhoto(
                sourceUri = Uri.parse(avatarUri),
                userId = userId
            )
        }

        try {
            val updatedMetadata = buildJsonObject {
                // Username + avatar always win. Other keys (if any) are
                // intentionally not preserved since this app only stores
                // those two fields in userMetadata.
                username?.let { put("username", JsonPrimitive(it)) }
                if (deleteAvatar) {
                    // GoTrue merges user metadata, so omitting the key would
                    // leave the old avatar in place. An explicit JSON null
                    // actually clears it.
                    put("avatar", JsonNull)
                } else {
                    resolvedAvatar
                        ?.takeUnless { it.startsWith("content://") }
                        ?.let { put("avatar", JsonPrimitive(it)) }
                }
            }

            supabaseClient.auth.updateUser {
                this.data = updatedMetadata
            }
        } catch (_: Exception) {
            // Offline or transient — caller's local Room copy is still saved.
        }

        return resolvedAvatar
    }

    /**
     * Fetches the user's username + avatar URL from Supabase user metadata.
     */
    suspend fun loadProfile(): ProfileData? {
        return try {
            val user = supabaseClient.auth.retrieveUserForCurrentSession()
            val metadata = user.userMetadata
            val username = metadata?.get("username")?.jsonPrimitiveContentOrNull()
            // Older builds could write a device-only content:// URI when the
            // upload failed. Treat those as "no remote avatar".
            val avatar = metadata?.get("avatar")
                ?.jsonPrimitiveContentOrNull()
                ?.takeUnless { it.startsWith("content://") }
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
