package com.smartexpense.reports.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpense.core.common.toDateString
import com.smartexpense.designsystems.LivelyBlue
import com.smartexpense.designsystems.LivelyChoicePill
import com.smartexpense.designsystems.LivelyCyan
import com.smartexpense.designsystems.LivelyGradientButton
import com.smartexpense.designsystems.LivelyGreen
import com.smartexpense.designsystems.LivelyIndigo
import com.smartexpense.designsystems.LivelyPink
import com.smartexpense.designsystems.LivelyRose
import com.smartexpense.designsystems.LivelySectionCard
import com.smartexpense.designsystems.LivelyTeal
import com.smartexpense.designsystems.LivelyViolet
import com.smartexpense.designsystems.ModernEmptyState
import com.smartexpense.designsystems.ModernHeader
import com.smartexpense.designsystems.ModernSectionHeader
import com.smartexpense.domain.export.ReportExportFormat
import com.smartexpense.domain.export.ReportExportType
import com.smartexpense.reports.component.CategorySpendingChart
import com.smartexpense.reports.component.DateRangeCalendarDialog
import com.smartexpense.reports.component.DailySpendingChart
import com.smartexpense.reports.effect.ReportsEffect
import com.smartexpense.reports.export.ReportShareIntentFactory
import com.smartexpense.reports.export.displayName
import com.smartexpense.reports.intent.ReportsIntent
import com.smartexpense.reports.state.ReportsUiState
import com.smartexpense.reports.viewmodel.ReportsViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

private val ReportRoseEnd = Color(0xFF1E3A8A)

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember {
        SnackbarHostState()
    }

    val saveDocumentLauncher = rememberLauncherForActivityResult(
        contract = CreateReportDocumentContract()
    ) { uri ->
        if (uri == null) {
            viewModel.onIntent(
                ReportsIntent.SaveDestinationSelectionCancelled
            )
        } else {
            viewModel.onIntent(
                ReportsIntent.SaveDestinationSelected(uri.toString())
            )
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ReportsEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }

                is ReportsEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }

                is ReportsEffect.LaunchSaveDocument -> {
                    try {
                        saveDocumentLauncher.launch(
                            ReportDocumentRequest(
                                fileName = effect.fileName,
                                mimeType = effect.mimeType
                            )
                        )
                    } catch (exception: Exception) {
                        viewModel.onIntent(
                            ReportsIntent.ExportLaunchFailed(
                                exception.message
                                    ?: "Could not open the file picker"
                            )
                        )
                    }
                }

                is ReportsEffect.LaunchShare -> {
                    try {
                        val shareIntent = ReportShareIntentFactory.create(
                            context = context,
                            fileUri = effect.fileUri,
                            mimeType = effect.mimeType
                        )

                        context.startActivity(
                            Intent.createChooser(shareIntent, effect.title)
                        )
                    } catch (exception: Exception) {
                        viewModel.onIntent(
                            ReportsIntent.ExportLaunchFailed(
                                exception.message
                                    ?: "Could not open the share sheet"
                            )
                        )
                    }
                }
            }
        }
    }

    ReportsContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent
    )
}

@Composable
private fun ReportsContent(
    uiState: ReportsUiState,
    snackbarHostState: SnackbarHostState,
    onIntent: (ReportsIntent) -> Unit
) {
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        // MainScreen already handles system bar insets. Applying them here
        // again pushed the header too far down from the top.
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ModernHeader(
                title = "Reports",
                subtitle = formatDateRange(
                    uiState.startDateInclusive,
                    uiState.endDateExclusive
                ),
                icon = Icons.Default.BarChart
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.error != null) {
                    item {
                        ReportErrorCard(
                            message = uiState.error,
                            onRetry = { onIntent(ReportsIntent.Retry) }
                        )
                    }
                } else if (uiState.isLoading) {
                    item {
                        ReportLoadingCard()
                    }
                } else {
                    item {
                        DateRangeCard(
                            uiState = uiState,
                            onIntent = onIntent
                        )
                    }

                    item {
                        ExportCard(
                            uiState = uiState,
                            onIntent = onIntent
                        )
                    }

                    item {
                        SummaryCard(uiState = uiState)
                    }

                    item {
                        ModernSectionHeader(title = "Spending by Category")
                    }

                    if (uiState.report.categoryBreakdown.isEmpty()) {
                        item {
                            ModernEmptyState(
                                icon = Icons.Default.Category,
                                title = "No category spending",
                                subtitle = "No category spending for this period."
                            )
                        }
                    } else {
                        item {
                            CategorySpendingChart(
                                categoryBreakdown = uiState.report.categoryBreakdown,
                                categoryNames = uiState.categoryNames
                            )
                        }
                    }

                    item {
                        ModernSectionHeader(title = "Daily Spending")
                    }

                    if (uiState.report.dailySpending.isEmpty()) {
                        item {
                            ModernEmptyState(
                                icon = Icons.Default.Receipt,
                                title = "No daily spending",
                                subtitle = "No daily spending for this period."
                            )
                        }
                    } else {
                        item {
                            DailySpendingChart(
                                dailySpending = uiState.report.dailySpending
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        if (uiState.isDateRangePickerOpen) {
            DateRangeCalendarDialog(
                initialStartDateInclusive = uiState.startDateInclusive,
                initialEndDateExclusive = uiState.endDateExclusive,
                onDismiss = {
                    onIntent(ReportsIntent.DatePickerDismissed)
                },
                onRangeSelected = { startDateInclusive, endDateExclusive ->
                    onIntent(
                        ReportsIntent.DateRangeChanged(
                            startDateInclusive = startDateInclusive,
                            endDateExclusive = endDateExclusive
                        )
                    )
                }
            )
        }
    }
}

// ───────────────────────────────────────────────────────────────────────
// Date range
// ───────────────────────────────────────────────────────────────────────

@Composable
private fun DateRangeCard(
    uiState: ReportsUiState,
    onIntent: (ReportsIntent) -> Unit
) {
    LivelySectionCard(
        accent = LivelyBlue,
        accentEnd = LivelyCyan,
        title = "Reporting period",
        subtitle = formatDateRange(
            uiState.startDateInclusive,
            uiState.endDateExclusive
        ),
        icon = Icons.Default.DateRange
    ) {
        val rangeShape = RoundedCornerShape(18.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(rangeShape)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            LivelyBlue.copy(alpha = 0.14f),
                            LivelyViolet.copy(alpha = 0.10f)
                        )
                    )
                )
                .border(1.dp, LivelyBlue.copy(alpha = 0.28f), rangeShape)
                .clickable {
                    onIntent(ReportsIntent.DateRangePickerOpened)
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RangeEndpoint(
                label = "START",
                date = formatSingleDate(uiState.startDateInclusive),
                accent = LivelyBlue,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(18.dp),
                tint = LivelyViolet
            )

            RangeEndpoint(
                label = "END",
                date = formatSingleDate(uiState.endDateExclusive - 1L),
                accent = LivelyViolet,
                modifier = Modifier.weight(1f),
                alignment = Alignment.End
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = LivelyCyan
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = formatRangeLength(
                        uiState.startDateInclusive,
                        uiState.endDateExclusive
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                onClick = {
                    onIntent(ReportsIntent.DateRangePickerOpened)
                },
                shape = RoundedCornerShape(percent = 50),
                color = LivelyIndigo.copy(alpha = 0.12f),
                contentColor = LivelyIndigo
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 7.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EditCalendar,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "Change range",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun RangeEndpoint(
    label: String,
    date: String,
    accent: Color,
    modifier: Modifier = Modifier,
    alignment: Alignment.Horizontal = Alignment.Start
) {
    Column(
        modifier = modifier,
        horizontalAlignment = alignment
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = accent
        )

        Spacer(Modifier.height(3.dp))

        Text(
            text = date,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ───────────────────────────────────────────────────────────────────────
// Export
// ───────────────────────────────────────────────────────────────────────

@Composable
private fun ExportCard(
    uiState: ReportsUiState,
    onIntent: (ReportsIntent) -> Unit
) {
    val controlsEnabled = !uiState.isExporting
    val exportEnabled = controlsEnabled &&
        !uiState.isLoading &&
        uiState.endDateExclusive > uiState.startDateInclusive

    val typeLabel = when (uiState.exportType) {
        ReportExportType.TRANSACTIONS -> "Transactions"
        ReportExportType.SUMMARY -> "Summary"
    }
    val formatLabel = uiState.exportFormat.displayName

    LivelySectionCard(
        accent = LivelyIndigo,
        accentEnd = LivelyViolet,
        title = "Export Report",
        subtitle = "Choose what to export and how to share it",
        icon = Icons.Default.FileDownload
    ) {
        Text(
            text = "Report type",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LivelyChoicePill(
                label = "Transactions",
                selected = uiState.exportType == ReportExportType.TRANSACTIONS,
                accent = LivelyIndigo,
                accentEnd = LivelyViolet,
                icon = Icons.Default.Receipt,
                enabled = controlsEnabled,
                role = Role.RadioButton,
                modifier = Modifier.weight(1f),
                onClick = {
                    onIntent(
                        ReportsIntent.ExportTypeSelected(ReportExportType.TRANSACTIONS)
                    )
                }
            )

            LivelyChoicePill(
                label = "Summary",
                selected = uiState.exportType == ReportExportType.SUMMARY,
                accent = LivelyIndigo,
                accentEnd = LivelyViolet,
                icon = Icons.Default.BarChart,
                enabled = controlsEnabled,
                role = Role.RadioButton,
                modifier = Modifier.weight(1f),
                onClick = {
                    onIntent(
                        ReportsIntent.ExportTypeSelected(ReportExportType.SUMMARY)
                    )
                }
            )
        }

        Text(
            text = "File format",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReportExportFormat.entries.forEach { format ->
                val icon = when (format) {
                    ReportExportFormat.CSV -> Icons.Default.GridOn
                    ReportExportFormat.PDF -> Icons.Default.PictureAsPdf
                }

                LivelyChoicePill(
                    label = format.displayName,
                    selected = uiState.exportFormat == format,
                    accent = LivelyGreen,
                    accentEnd = LivelyTeal,
                    icon = icon,
                    enabled = controlsEnabled,
                    role = Role.RadioButton,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onIntent(ReportsIntent.ExportFormatSelected(format))
                    }
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = LivelyIndigo.copy(alpha = 0.10f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.GridOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = LivelyIndigo
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "$typeLabel • $formatLabel",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = LivelyIndigo
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SoftActionButton(
                text = "Save $formatLabel",
                icon = Icons.Default.SaveAlt,
                enabled = exportEnabled,
                modifier = Modifier.weight(1f),
                onClick = { onIntent(ReportsIntent.SaveClicked) }
            )

            LivelyGradientButton(
                text = "Share $formatLabel",
                accent = LivelyIndigo,
                accentEnd = LivelyViolet,
                icon = Icons.Default.Share,
                enabled = exportEnabled,
                modifier = Modifier.weight(1f),
                onClick = { onIntent(ReportsIntent.ShareClicked) }
            )
        }

        if (uiState.isExporting) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = LivelyIndigo
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Preparing export...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SoftActionButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    val contentColor = if (enabled) {
        LivelyIndigo
    } else {
        colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                LivelyIndigo.copy(alpha = if (enabled) 0.10f else 0.05f)
            )
            .border(
                width = 1.dp,
                color = LivelyIndigo.copy(alpha = if (enabled) 0.45f else 0.2f),
                shape = shape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = contentColor
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ───────────────────────────────────────────────────────────────────────
// Summary
// ───────────────────────────────────────────────────────────────────────

@Composable
private fun SummaryCard(
    uiState: ReportsUiState
) {
    val report = uiState.report

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            LivelyIndigo,
                            LivelyViolet,
                            LivelyPink
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = CircleShape,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                    }
                }

                Text(
                    text = "Total Spending",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Text(
                text = formatCurrency(report.totalAmount),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryMetric(
                    label = "Expenses",
                    value = report.expenseCount.toString(),
                    modifier = Modifier.weight(1f)
                )

                SummaryMetric(
                    label = "Average",
                    value = formatCurrency(report.averageAmount),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f)
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

// ───────────────────────────────────────────────────────────────────────
// Loading / error
// ───────────────────────────────────────────────────────────────────────

@Composable
private fun ReportLoadingCard() {
    LivelySectionCard(
        accent = LivelyBlue,
        accentEnd = LivelyCyan
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = LivelyBlue
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Loading report...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReportErrorCard(
    message: String,
    onRetry: () -> Unit
) {
    LivelySectionCard(
        accent = LivelyRose,
        accentEnd = ReportRoseEnd,
        title = "Couldn't load report",
        subtitle = message,
        icon = Icons.Default.ErrorOutline
    ) {
        LivelyGradientButton(
            text = "Retry",
            accent = LivelyRose,
            accentEnd = ReportRoseEnd,
            icon = Icons.Default.Refresh,
            onClick = onRetry
        )
    }
}


// ───────────────────────────────────────────────────────────────────────
// Export plumbing
// ───────────────────────────────────────────────────────────────────────

private data class ReportDocumentRequest(
    val fileName: String,
    val mimeType: String
)

private class CreateReportDocumentContract :
    ActivityResultContract<ReportDocumentRequest, Uri?>() {

    override fun createIntent(
        context: Context,
        input: ReportDocumentRequest
    ): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = input.mimeType
            putExtra(Intent.EXTRA_TITLE, input.fileName)
        }
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): Uri? {
        return if (resultCode == Activity.RESULT_OK) {
            intent?.data
        } else {
            null
        }
    }
}

// ───────────────────────────────────────────────────────────────────────
// Formatting
// ───────────────────────────────────────────────────────────────────────

private fun formatCurrency(amount: Double): String {
    return NumberFormat.getCurrencyInstance(
        Locale("en", "PH")
    ).format(amount)
}

private fun formatSingleDate(millis: Long): String {
    return if (millis <= 0L) "Select date" else millis.toDateString()
}

private fun formatDateRange(
    startDateInclusive: Long,
    endDateExclusive: Long
): String {
    if (startDateInclusive == 0L || endDateExclusive == 0L) {
        return "Selected period"
    }

    val start = startDateInclusive.toDateString()
    val end = (endDateExclusive - 1L).toDateString()

    return "$start – $end"
}

private fun formatRangeLength(
    startDateInclusive: Long,
    endDateExclusive: Long
): String {
    if (startDateInclusive <= 0L || endDateExclusive <= startDateInclusive) {
        return "No dates selected"
    }

    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(startDateInclusive)
        .atZone(zone)
        .toLocalDate()
    val end = Instant.ofEpochMilli(endDateExclusive - 1L)
        .atZone(zone)
        .toLocalDate()
    val days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1

    return if (days == 1L) "1 day selected" else "$days days selected"
}
