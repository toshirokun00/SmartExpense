package com.smartexpense.data.remote

import android.net.Uri
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
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
            val existingMetadata = user.userMetadata
            val updatedMetadata = buildJsonObject {
                // Keep metadata written by other features (for example the
                // Spendora account ID) instead of dropping unknown keys.
                existingMetadata?.forEach { (key, value) ->
                    put(key, value)
                }
                // Username + avatar always win.
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
            val spendoraId = metadata?.get(SPENDORA_ID_KEY)
                ?.jsonPrimitiveContentOrNull()
                ?.takeIf { it.isNotBlank() }
            val emailVisible = metadata?.get(EMAIL_VISIBLE_KEY)
                ?.jsonPrimitiveBooleanOrNull()
            val result = ProfileData(username, avatar, spendoraId, emailVisible)
            if (
                result.username == null &&
                result.avatarUri == null &&
                result.spendoraId == null &&
                result.isEmailVisible == null
            ) {
                null
            } else {
                result
            }
        } catch (_: Exception) {
            null
        }
    }

    /** Reads the public Spendora account ID from user metadata. */
    suspend fun loadSpendoraId(): String? {
        return try {
            val user = supabaseClient.auth.retrieveUserForCurrentSession()
            user.userMetadata
                ?.get(SPENDORA_ID_KEY)
                ?.jsonPrimitiveContentOrNull()
                ?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    /** Persists the public Spendora account ID in user metadata. */
    suspend fun saveSpendoraId(spendoraId: String) {
        val user = supabaseClient.auth.currentUserOrNull() ?: return
        val updatedMetadata = buildJsonObject {
            user.userMetadata?.forEach { (key, value) ->
                put(key, value)
            }
            put(SPENDORA_ID_KEY, JsonPrimitive(spendoraId))
        }
        supabaseClient.auth.updateUser {
            this.data = updatedMetadata
        }
    }

    /** Reads the saved email visibility preference from user metadata. */
    suspend fun loadEmailVisibility(): Boolean? {
        return try {
            val user = supabaseClient.auth.retrieveUserForCurrentSession()
            user.userMetadata
                ?.get(EMAIL_VISIBLE_KEY)
                ?.jsonPrimitiveBooleanOrNull()
        } catch (_: Exception) {
            null
        }
    }

    /** Persists the email visibility preference in user metadata. */
    suspend fun saveEmailVisibility(visible: Boolean) {
        val user = supabaseClient.auth.currentUserOrNull() ?: return
        val updatedMetadata = buildJsonObject {
            user.userMetadata?.forEach { (key, value) ->
                put(key, value)
            }
            put(EMAIL_VISIBLE_KEY, JsonPrimitive(visible))
        }
        supabaseClient.auth.updateUser {
            this.data = updatedMetadata
        }
    }

    data class ProfileData(
        val username: String?,
        val avatarUri: String?,
        val spendoraId: String? = null,
        val isEmailVisible: Boolean? = null
    )

    private companion object {
        const val SPENDORA_ID_KEY = "spendora_id"
        const val EMAIL_VISIBLE_KEY = "email_visible"
    }

    private fun kotlinx.serialization.json.JsonElement.jsonPrimitiveContentOrNull(): String? =
        (this as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull

    private fun kotlinx.serialization.json.JsonElement.jsonPrimitiveBooleanOrNull(): Boolean? =
        (this as? kotlinx.serialization.json.JsonPrimitive)?.booleanOrNull
}
