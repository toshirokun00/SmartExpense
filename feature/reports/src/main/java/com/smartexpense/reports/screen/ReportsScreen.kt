package com.smartexpense.reports.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpense.core.common.toDateString
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Expense Reports",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formatDateRange(
                    uiState.startDateInclusive,
                    uiState.endDateExclusive
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isExporting,
                    onClick = {
                        onIntent(ReportsIntent.StartDatePickerOpened)
                    }
                ) {
                    Text("Start Date")
                }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isExporting,
                    onClick = {
                        onIntent(ReportsIntent.EndDatePickerOpened)
                    }
                ) {
                    Text("End Date")
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

            Spacer(modifier = Modifier.height(12.dp))

            ExportCard(
                uiState = uiState,
                onIntent = onIntent
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                uiState.isLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading report...")
                    }
                }

                uiState.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = uiState.error,
                            color = MaterialTheme.colorScheme.error
                        )

                        TextButton(
                            onClick = {
                                onIntent(ReportsIntent.Retry)
                            }
                        ) {
                            Text("Retry")
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            SummaryCard(uiState = uiState)
                        }


                        item {
                            SectionTitle("Spending by Category")
                        }

                        item {
                            CategorySpendingChart(
                                categoryBreakdown = uiState.report.categoryBreakdown,
                                categoryNames = uiState.categoryNames
                            )
                        }


                        if (uiState.report.categoryBreakdown.isEmpty()) {
                            item {
                                EmptySectionMessage(
                                    message = "No category spending for this period."
                                )
                            }
                        } else {
                            items(
                                items = uiState.report.categoryBreakdown,
                                key = { it.categoryId }
                            ) { category ->
                                CategorySpendingItem(
                                    category = category,
                                    categoryName = uiState.categoryNames[category.categoryId]
                                        ?: "Unknown Category"
                                )
                            }
                        }

                        item {
                            SectionTitle("Daily Spending")
                        }

                        item {
                            DailySpendingChart(
                                dailySpending = uiState.report.dailySpending
                            )
                        }


                        if (uiState.report.dailySpending.isEmpty()) {
                            item {
                                EmptySectionMessage(
                                    message = "No daily spending for this period."
                                )
                            }
                        } else {
                            items(
                                items = uiState.report.dailySpending,
                                key = { it.date }
                            ) { daily ->
                                DailySpendingItem(daily = daily)
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportCard(
    uiState: ReportsUiState,
    onIntent: (ReportsIntent) -> Unit
) {
    val controlsEnabled = !uiState.isExporting
    val exportEnabled = controlsEnabled &&
        !uiState.isLoading &&
        uiState.endDateExclusive > uiState.startDateInclusive

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Export Report",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Choose what to export and the file format.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.exportType ==
                        ReportExportType.TRANSACTIONS,
                    enabled = controlsEnabled,
                    onClick = {
                        onIntent(
                            ReportsIntent.ExportTypeSelected(
                                ReportExportType.TRANSACTIONS
                            )
                        )
                    },
                    label = {
                        Text("Transactions")
                    }
                )

                FilterChip(
                    selected = uiState.exportType == ReportExportType.SUMMARY,
                    enabled = controlsEnabled,
                    onClick = {
                        onIntent(
                            ReportsIntent.ExportTypeSelected(
                                ReportExportType.SUMMARY
                            )
                        )
                    },
                    label = {
                        Text("Summary")
                    }
                )

                listOf(
                    ReportExportFormat.CSV,
                    ReportExportFormat.PDF
                ).forEach { format ->
                    FilterChip(
                        selected = uiState.exportFormat == format,
                        enabled = controlsEnabled,
                        onClick = {
                            onIntent(
                                ReportsIntent.ExportFormatSelected(format)
                            )
                        },
                        label = {
                            Text(format.displayName)
                        }
                    )
                }
            }

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
                    Text("Save ${uiState.exportFormat.displayName}")
                }

                Button(
                    modifier = Modifier.weight(1f),
                    enabled = exportEnabled,
                    onClick = {
                        onIntent(ReportsIntent.ShareClicked)
                    }
                ) {
                    Text("Share ${uiState.exportFormat.displayName}")
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
private fun SectionTitle(
    title: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun CategorySpendingItem(
    category: CategorySpending,
    categoryName: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = formatCurrency(category.totalAmount),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = "${"%.1f".format(Locale.US, category.percentage)}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DailySpendingItem(
    daily: DailySpending
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = daily.date.toDateString(),
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = formatCurrency(daily.totalAmount),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun EmptySectionMessage(
    message: String
) {
    Text(
        text = message,
        modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium
    )
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

/**
 * A stable CreateDocument contract whose MIME type is supplied per launch.
 *
 * Using a fixed contract instance keeps the launcher registration stable while
 * still letting CSV and PDF exports request the correct document type.
 */
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