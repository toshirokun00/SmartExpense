package com.smartexpense.reports

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartexpense.domain.export.ReportExportContent
import com.smartexpense.domain.export.ReportExportFile
import com.smartexpense.domain.export.ReportExportFormat
import com.smartexpense.domain.export.ReportExportType
import com.smartexpense.reports.export.AndroidReportExportFileWriter
import com.smartexpense.reports.export.ReportShareIntentFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ReportShareIntentFactoryTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun tearDown() {
        File(
            context.cacheDir,
            AndroidReportExportFileWriter.EXPORT_DIRECTORY
        ).deleteRecursively()
    }

    @Test
    fun shareIntentUsesContentUriAndTemporaryReadPermission() = runBlocking {
        val writer = AndroidReportExportFileWriter(context)
        val shared = writer.createShareFile(
            export = ReportExportFile(
                type = ReportExportType.TRANSACTIONS,
                format = ReportExportFormat.CSV,
                fileName = "smartexpense_transactions_2026-01-01_2026-01-31.csv",
                content = ReportExportContent.Csv("a,b"),
                recordCount = 1
            )
        )

        val intent = ReportShareIntentFactory.create(
            context = context,
            fileUri = shared.uri,
            mimeType = shared.mimeType
        )

        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("text/csv", intent.type)
        assertTrue(
            intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0
        )

        val streamUri = IntentCompat.getParcelableExtra(
            intent,
            Intent.EXTRA_STREAM,
            Uri::class.java
        )
        assertEquals(Uri.parse(shared.uri), streamUri)

        assertNotNull(intent.clipData)
        assertEquals(
            Uri.parse(shared.uri),
            intent.clipData?.getItemAt(0)?.uri
        )
    }

    @Test
    fun shareIntentCarriesPdfMimeType() {
        val fileUri = "content://com.smartexpense.fileprovider/" +
            "report_exports/smartexpense_report_2026-01-01_2026-01-31.pdf"

        val intent = ReportShareIntentFactory.create(
            context = context,
            fileUri = fileUri,
            mimeType = "application/pdf"
        )

        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("application/pdf", intent.type)
        assertEquals(
            Uri.parse(fileUri),
            IntentCompat.getParcelableExtra(
                intent,
                Intent.EXTRA_STREAM,
                Uri::class.java
            )
        )
        assertTrue(
            intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0
        )
    }
}
