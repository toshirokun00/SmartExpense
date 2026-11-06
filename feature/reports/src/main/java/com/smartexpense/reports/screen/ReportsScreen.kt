package com.smartexpense.reports.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpense.core.common.toDateString
import com.smartexpense.designsystems.ModernEmptyState
import com.smartexpense.designsystems.ModernHeader
import com.smartexpense.designsystems.ModernListItemCard
import com.smartexpense.designsystems.ModernSectionHeader
import com.smartexpense.domain.export.ReportExportFormat
import com.smartexpense.domain.export.ReportExportType
import com.smartexpense.domain.model.CategorySpending
import com.smartexpense.domain.model.DailySpending
import com.smartexpense.reports.component.CategorySpendingChart
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
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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

                item {
                    CategorySpendingChart(
                        categoryBreakdown = uiState.report.categoryBreakdown,
                        categoryNames = uiState.categoryNames
                    )
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
                    items(
                        items = uiState.report.categoryBreakdown,
                        key = { it.categoryId }
                    ) { category ->
                        ModernListItemCard(
                            avatarIcon = Icons.Default.Category,
                            avatarColor = MaterialTheme.colorScheme.primary,
                            title = uiState.categoryNames[category.categoryId]
                                ?: "Unknown Category",
                            subtitle = "${"%.1f".format(Locale.US, category.percentage)}% of total",
                            trailing = {
                                Text(
                                    text = formatCurrency(category.totalAmount),
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            borderless = true
                        )
                    }
                }

                item {
                    ModernSectionHeader(title = "Daily Spending")
                }

                item {
                    DailySpendingChart(
                        dailySpending = uiState.report.dailySpending
                    )
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
                    items(
                        items = uiState.report.dailySpending,
                        key = { it.date }
                    ) { daily ->
                        ModernListItemCard(
                            avatarIcon = Icons.Default.Receipt,
                            avatarColor = MaterialTheme.colorScheme.secondary,
                            title = daily.date.toDateString(),
                            subtitle = formatCurrency(daily.totalAmount),
                            borderless = true
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        if (uiState.isStartDatePickerOpen) {
            ReportDatePickerDialog(
                initialDate = uiState.startDateInclusive,
                title = "Select start date",
                onDismiss = {
                    onIntent(ReportsIntent.DatePickerDismissed)
                },
                onDateSelected = { selectedMillis ->
                    val zone = ZoneId.systemDefault()
                    val selectedStart = Instant.ofEpochMilli(selectedMillis)
                        .atZone(zone)
                        .toLocalDate()
                        .atStartOfDay(zone)
                        .toInstant()
                        .toEpochMilli()

                    onIntent(
                        ReportsIntent.DateRangeChanged(
                            startDateInclusive = selectedStart,
                            endDateExclusive = uiState.endDateExclusive
                        )
                    )
                }
            )
        }

        if (uiState.isEndDatePickerOpen) {
            ReportDatePickerDialog(
                initialDate = uiState.endDateExclusive - 1L,
                title = "Select end date",
                onDismiss = {
                    onIntent(ReportsIntent.DatePickerDismissed)
                },
                onDateSelected = { selectedMillis ->
                    val zone = ZoneId.systemDefault()
                    val selectedEndDate = Instant.ofEpochMilli(selectedMillis)
                        .atZone(zone)
                        .toLocalDate()
                        .plusDays(1)
                        .atStartOfDay(zone)
                        .toInstant()
                        .toEpochMilli()

                    onIntent(
                        ReportsIntent.DateRangeChanged(
                            startDateInclusive = uiState.startDateInclusive,
                            endDateExclusive = selectedEndDate
                        )
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportCard(
    uiState: ReportsUiState,
    onIntent: (ReportsIntent) -> Unit
) {
    val controlsEnabled = !uiState.isExporting
    val exportEnabled = controlsEnabled &&
        !uiState.isLoading &&
        uiState.endDateExclusive > uiState.startDateInclusive

    var showTypeDropdown by remember { mutableStateOf(false) }
    var showFormatDropdown by remember { mutableStateOf(false) }

    val currentTypeLabel = when (uiState.exportType) {
        ReportExportType.TRANSACTIONS -> "Transactions"
        ReportExportType.SUMMARY -> "Summary"
    }

    val currentFormatLabel = uiState.exportFormat.displayName

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = "Export Report",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // ── Type dropdown ─────────────────────────────────────────────
            ExposedDropdownMenuBox(
                expanded = showTypeDropdown,
                onExpandedChange = {
                    if (controlsEnabled) showTypeDropdown = !showTypeDropdown
                }
            ) {
                OutlinedTextField(
                    value = currentTypeLabel,
                    onValueChange = {},
                    readOnly = true,
                    enabled = controlsEnabled,
                    label = {
                        Text("Report type")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = showTypeDropdown
                        )
                    }
                )

                ExposedDropdownMenu(
                    expanded = showTypeDropdown,
                    onDismissRequest = {
                        showTypeDropdown = false
                    }
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text("Transactions", fontWeight = FontWeight.Medium)
                                if (uiState.exportType == ReportExportType.TRANSACTIONS) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        onClick = {
                            onIntent(
                                ReportsIntent.ExportTypeSelected(
                                    ReportExportType.TRANSACTIONS
                                )
                            )
                            showTypeDropdown = false
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text("Summary", fontWeight = FontWeight.Medium)
                                if (uiState.exportType == ReportExportType.SUMMARY) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        onClick = {
                            onIntent(
                                ReportsIntent.ExportTypeSelected(
                                    ReportExportType.SUMMARY
                                )
                            )
                            showTypeDropdown = false
                        }
                    )
                }
            }

            // ── Format dropdown ───────────────────────────────────────────
            ExposedDropdownMenuBox(
                expanded = showFormatDropdown,
                onExpandedChange = {
                    if (controlsEnabled) showFormatDropdown = !showFormatDropdown
                }
            ) {
                OutlinedTextField(
                    value = currentFormatLabel,
                    onValueChange = {},
                    readOnly = true,
                    enabled = controlsEnabled,
                    label = {
                        Text("File format")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = showFormatDropdown
                        )
                    }
                )

                ExposedDropdownMenu(
                    expanded = showFormatDropdown,
                    onDismissRequest = {
                        showFormatDropdown = false
                    }
                ) {
                    ReportExportFormat.entries.forEach { format ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        format.displayName,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (uiState.exportFormat == format) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onIntent(
                                    ReportsIntent.ExportFormatSelected(format)
                                )
                                showFormatDropdown = false
                            }
                        )
                    }
                }
            }

            // ── Action buttons ────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = exportEnabled,
                    onClick = {
                        onIntent(ReportsIntent.SaveClicked)
                    }
                ) {
                    Text("Save")
                }

                Button(
                    modifier = Modifier.weight(1f),
                    enabled = exportEnabled,
                    onClick = {
                        onIntent(ReportsIntent.ShareClicked)
                    }
                ) {
                    Text("Share")
                }
            }

            if (uiState.isExporting) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )

                    Text(
                        text = "Preparing export...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    uiState: ReportsUiState
) {
    val report = uiState.report

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Total Spending",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = formatCurrency(report.totalAmount),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                    alpha = 0.2f
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryMetric(
                    label = "Expenses",
                    value = report.expenseCount.toString()
                )

                SummaryMetric(
                    label = "Average",
                    value = formatCurrency(report.averageAmount)
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ReportDatePickerDialog(
    initialDate: Long,
    title: String,
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let(onDateSelected)
                }
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            DatePicker(state = pickerState)
        }
    }
}

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

private fun formatCurrency(amount: Double): String {
    return NumberFormat.getCurrencyInstance(
        Locale("en", "PH")
    ).format(amount)
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
