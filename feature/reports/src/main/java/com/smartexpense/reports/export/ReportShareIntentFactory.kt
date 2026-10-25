package com.smartexpense.reports.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Builds a secure ACTION_SEND intent for an exported CSV or PDF.
 *
 * The URI must be a FileProvider content URI. ClipData and the read grant make
 * the temporary permission available to whichever share target the user picks.
 */
object ReportShareIntentFactory {

    fun create(
        context: Context,
        fileUri: String,
        mimeType: String
    ): Intent {
        val uri = Uri.parse(fileUri)

        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(
                context.contentResolver,
                "SmartExpense export",
                uri
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
