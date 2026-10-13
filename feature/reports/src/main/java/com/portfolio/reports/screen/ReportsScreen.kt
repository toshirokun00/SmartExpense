package com.portfolio.reports.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.portfolio.reports.component.CategorySpendingChart
import com.portfolio.reports.component.DailySpendingChart
import com.portfolio.reports.effect.ReportsEffect
import com.portfolio.reports.intent.ReportsIntent
import com.portfolio.reports.state.ReportsUiState
import com.portfolio.reports.viewmodel.ReportsViewModel
import com.smartexpense.domain.model.CategorySpending
import com.smartexpense.domain.model.DailySpending
import org.koin.androidx.compose.koinViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember {
        SnackbarHostState()
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ReportsEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
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
                    onClick = {
                        onIntent(ReportsIntent.StartDatePickerOpened)
                    }
                ) {
                    Text("Start Date")
                }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
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
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            SummaryCard(uiState = uiState)
                        }

                        item {
                            SectionTitle("Spending by Category")
                        }

                        item {
                            SectionTitle("Daily Spending")
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
                            DailySpendingChart(
                                dailySpending = uiState.report.dailySpending
                            )
                        }


                        item {
                            SectionTitle("Daily Spending")
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
                text = formatDate(daily.date),
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

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat(
        "MMM dd, yyyy",
        Locale.getDefault()
    ).format(Date(timestamp))
}

private fun formatDateRange(
    startDateInclusive: Long,
    endDateExclusive: Long
): String {
    if (startDateInclusive == 0L || endDateExclusive == 0L) {
        return "Selected period"
    }

    val start = formatDate(startDateInclusive)
    val end = formatDate(endDateExclusive - 1L)

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