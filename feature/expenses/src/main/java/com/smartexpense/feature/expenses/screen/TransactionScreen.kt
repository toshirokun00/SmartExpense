package com.smartexpense.feature.expenses.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpense.core.common.toDateString
import com.smartexpense.core.common.toPesoAmount
import com.smartexpense.domain.model.Category
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.model.ExpenseSyncStatus
import com.smartexpense.domain.model.SortOrder
import com.smartexpense.feature.expenses.effect.TransactionEffect
import com.smartexpense.feature.expenses.intent.TransactionIntent
import com.smartexpense.feature.expenses.state.TransactionUiState
import com.smartexpense.feature.expenses.viewmodel.TransactionViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    viewModel: TransactionViewModel = koinViewModel(),
    onExpenseClick: (Long) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TransactionEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }

                TransactionEffect.SyncRetryRequested -> {
                    snackbarHostState.showSnackbar("Sync retry requested")
                }
            }
        }
    }

    // Local UI state only for transient Compose interactions (dialog visibility)
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val startDatePickerState = rememberDatePickerState()
    val endDatePickerState = rememberDatePickerState()

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Text(
                text = "Transactions",
                style = MaterialTheme.typography.headlineMedium
            )

            // Search field
            OutlinedTextField(
                value = uiState.filter.tempQuery,
                onValueChange = {
                    viewModel.onIntent(
                        TransactionIntent.TempSearchChanged(it)
                    )
                },
                label = { Text("Search description") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    autoCorrect = false
                )
            )

            // Filter toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.titleMedium
                )

                IconButton(
                    onClick = {
                        viewModel.onIntent(TransactionIntent.ToggleFilterSection)
                    }
                ) {
                    Text(if (uiState.filter.isSectionExpanded) "▲" else "▼")
                }
            }

            // Filter section (expandable and scrollable)
            if (uiState.filter.isSectionExpanded) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Category filter
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Category",
                                style = MaterialTheme.typography.titleSmall
                            )

                            uiState.categories.forEach { category ->
                                val isSelected = uiState.filter.tempSelectedCategoryIds.contains(category.id)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                viewModel.onIntent(
                                                    TransactionIntent.TempCategorySelectionChanged(
                                                        uiState.filter.tempSelectedCategoryIds + category.id
                                                    )
                                                )
                                            } else {
                                                viewModel.onIntent(
                                                    TransactionIntent.TempCategorySelectionChanged(
                                                        uiState.filter.tempSelectedCategoryIds - category.id
                                                    )
                                                )
                                            }
                                        }
                                    )
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            if (uiState.categories.isEmpty()) {
                                Text(
                                    text = "No categories available",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Date range filter
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Date Range",
                                style = MaterialTheme.typography.titleSmall
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        showStartDatePicker = true
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = uiState.filter.tempStartTime?.toDateString() ?: "Start Date"
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        showEndDatePicker = true
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = uiState.filter.tempEndTime?.toDateString() ?: "End Date"
                                    )
                                }
                            }

                            val startTs = uiState.filter.tempStartTime
                            val endTs = uiState.filter.tempEndTime
                            if (startTs != null && endTs != null && endTs < startTs) {
                                Text(
                                    text = "End date must be after start date",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        // Start date picker dialog
                        if (showStartDatePicker) {
                            DatePickerDialog(
                                onDismissRequest = {
                                    showStartDatePicker = false
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            viewModel.onIntent(
                                                TransactionIntent.TempDateRangeChanged(
                                                    startTime = startDatePickerState.selectedDateMillis,
                                                    endTime = uiState.filter.tempEndTime
                                                )
                                            )
                                            showStartDatePicker = false
                                        }
                                    ) {
                                        Text("Select")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = {
                                        showStartDatePicker = false
                                    }) {
                                        Text("Cancel")
                                    }
                                }
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Select Start Date",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    androidx.compose.material3.DatePicker(
                                        state = startDatePickerState
                                    )
                                }
                            }
                        }

                        // End date picker dialog
                        if (showEndDatePicker) {
                            DatePickerDialog(
                                onDismissRequest = {
                                    showEndDatePicker = false
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            viewModel.onIntent(
                                                TransactionIntent.TempDateRangeChanged(
                                                    startTime = uiState.filter.tempStartTime,
                                                    endTime = endDatePickerState.selectedDateMillis
                                                )
                                            )
                                            showEndDatePicker = false
                                        }
                                    ) {
                                        Text("Select")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = {
                                        showEndDatePicker = false
                                    }) {
                                        Text("Cancel")
                                    }
                                }
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Select End Date",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    androidx.compose.material3.DatePicker(
                                        state = endDatePickerState
                                    )
                                }
                            }
                        }

                        // Amount filter
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Amount Range",
                                style = MaterialTheme.typography.titleSmall
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = uiState.filter.tempMinAmount?.toString() ?: "",
                                    onValueChange = {
                                        val d = it.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
                                        viewModel.onIntent(
                                            TransactionIntent.TempAmountRangeChanged(
                                                minAmount = d,
                                                maxAmount = uiState.filter.tempMaxAmount
                                            )
                                        )
                                    },
                                    label = { Text("Min") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number
                                    )
                                )

                                OutlinedTextField(
                                    value = uiState.filter.tempMaxAmount?.toString() ?: "",
                                    onValueChange = {
                                        val d = it.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
                                        viewModel.onIntent(
                                            TransactionIntent.TempAmountRangeChanged(
                                                minAmount = uiState.filter.tempMinAmount,
                                                maxAmount = d
                                            )
                                        )
                                    },
                                    label = { Text("Max") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number
                                    )
                                )
                            }

                            val minAmount = uiState.filter.tempMinAmount
                            val maxAmount = uiState.filter.tempMaxAmount

                            if (minAmount != null && maxAmount != null && minAmount > maxAmount) {
                                Text(
                                    text = "Minimum amount must be less than maximum",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        // Sort filter
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Sort By",
                                style = MaterialTheme.typography.titleSmall
                            )

                            sortOrderOptions.forEach { (option, label) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = uiState.filter.tempSortOrder == option,
                                        onClick = {
                                            viewModel.onIntent(
                                                TransactionIntent.TempSortChanged(option)
                                            )
                                        }
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Apply / Cancel buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val startTs = uiState.filter.tempStartTime
                                    val endTs = uiState.filter.tempEndTime
                                    if (startTs != null && endTs != null && endTs < startTs) {
                                        return@Button
                                    }

                                    val minAmt = uiState.filter.tempMinAmount
                                    val maxAmt = uiState.filter.tempMaxAmount
                                    if (minAmt != null && maxAmt != null && minAmt > maxAmt) {
                                        return@Button
                                    }

                                    viewModel.onIntent(TransactionIntent.ApplyFilters)
                                }
                            ) {
                                Text("Apply")
                            }

                            Button(
                                onClick = {
                                    viewModel.onIntent(TransactionIntent.CancelFilters)
                                }
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }

            // Active filters indicator
            val hasActiveFilters =
                uiState.query.isNotEmpty() ||
                uiState.selectedCategoryIds.isNotEmpty() ||
                uiState.startTime != null ||
                uiState.endTime != null ||
                uiState.minAmount != null ||
                uiState.maxAmount != null ||
                uiState.sortOrder != SortOrder.NEWEST

            if (hasActiveFilters) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Active filters: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    var chipsShown = 0
                    val maxChips = 3

                    if (uiState.query.isNotEmpty() && chipsShown < maxChips) {
                        chipsShown++
                        Text(
                            text = "Search: ${uiState.query}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (uiState.selectedCategoryIds.isNotEmpty() && chipsShown < maxChips) {
                        chipsShown++
                        val categoryNames = uiState.categories
                            .filter { it.id in uiState.selectedCategoryIds }
                            .map { it.name }
                            .joinToString(", ")
                        Text(
                            text = "Category: $categoryNames",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if ((uiState.startTime != null || uiState.endTime != null) && chipsShown < maxChips) {
                        chipsShown++
                        Text(
                            text = "Date: ${uiState.startTime?.toDateString() ?: "—"} - ${uiState.endTime?.toDateString() ?: "—"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    TextButton(
                        onClick = {
                            viewModel.onIntent(TransactionIntent.ClearFilters)
                        }
                    ) {
                        Text("Clear All")
                    }
                }
            }

            // Results area
            if (uiState.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Loading transactions...")
                }
            } else if (uiState.expenses.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (hasActiveFilters) {
                            "No transactions match your filters"
                        } else {
                            "No transactions yet"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (hasActiveFilters) {
                        TextButton(
                            onClick = {
                                viewModel.onIntent(TransactionIntent.ClearFilters)
                            }
                        ) {
                            Text("Clear Filters")
                        }
                    }
                }
            } else {
                Text(
                    text = "${uiState.expenses.size} transaction${if (uiState.expenses.size != 1) "s" else ""}"
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.expenses,
                        key = { expense -> expense.id }
                    ) { expense ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                onExpenseClick(expense.id)
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = expense.merchant.ifBlank {
                                            expense.description.ifBlank { "Transaction" }
                                        },
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (
                                        expense.merchant.isNotBlank() &&
                                        expense.description.isNotBlank()
                                    ) {
                                        Text(
                                            text = expense.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = categoryName(expense.categoryId, uiState.categories),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = expense.date.toDateString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (expense.syncStatus != ExpenseSyncStatus.SYNCED) {
                                        Text(
                                            text = expense.syncStatus.label(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Column(
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = expense.amount.toPesoAmount(),
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    if (expense.syncStatus == ExpenseSyncStatus.FAILED) {
                                        TextButton(
                                            onClick = {
                                                viewModel.onIntent(
                                                    TransactionIntent.RetrySync(expense.id)
                                                )
                                            }
                                        ) {
                                            Text("Retry")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val sortOrderOptions: List<Pair<SortOrder, String>> = listOf(
    SortOrder.NEWEST to "Newest",
    SortOrder.OLDEST to "Oldest",
    SortOrder.HIGHEST_AMOUNT to "Highest Amount",
    SortOrder.LOWEST_AMOUNT to "Lowest Amount"
)

private fun categoryName(categoryId: Long, categories: List<Category>): String {
    return categories.firstOrNull { it.id == categoryId }?.name ?: "Unknown"
}

private fun formatAmountOrNull(amount: Double?): String {
    return amount?.toPesoAmount() ?: "—"
}
