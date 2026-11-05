package com.smartexpense.feature.expenses.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpense.core.common.toDateString
import com.smartexpense.core.common.toPesoAmount
import com.smartexpense.designsystems.ModernEmptyState
import com.smartexpense.designsystems.ModernHeader
import com.smartexpense.designsystems.ModernListItemCard
import com.smartexpense.designsystems.ModernSectionHeader
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
    var showFilterDialog by remember { mutableStateOf(false) }

    val startDatePickerState = rememberDatePickerState()
    val endDatePickerState = rememberDatePickerState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ModernHeader(
            title = "Transactions",
            subtitle = "Search and filter your expenses",
            icon = Icons.Default.Receipt,
            onBackClick = onNavigateBack,
            actions = {
                IconButton(onClick = { showFilterDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filters",
                        tint = Color.White
                    )
                }
            }
        )

        // Filter preview chip
        if (uiState.query.isNotEmpty() || uiState.selectedCategoryIds.isNotEmpty() ||
            uiState.startTime != null || uiState.endTime != null ||
            uiState.sortOrder != SortOrder.NEWEST) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Active filters",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                TextButton(
                    onClick = {
                        viewModel.onIntent(TransactionIntent.ClearFilters)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear", fontSize = 12.sp)
                }
            }
        }

        // Search bar
        OutlinedTextField(
            value = uiState.filter.tempQuery,
            onValueChange = {
                viewModel.onIntent(TransactionIntent.TempSearchChanged(it))
            },
            label = { Text("Search description") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(autoCorrect = false),
            shape = RoundedCornerShape(16.dp),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Loading transactions...")
                        }
                    }
                }
            } else if (uiState.expenses.isEmpty()) {
                item {
                    ModernEmptyState(
                        icon = Icons.Default.Receipt,
                        title = if (uiState.query.isNotEmpty() || uiState.selectedCategoryIds.isNotEmpty()) {
                            "No matches found"
                        } else {
                            "No transactions yet"
                        },
                        subtitle = if (uiState.query.isNotEmpty() || uiState.selectedCategoryIds.isNotEmpty()) {
                            "Try adjusting your filters"
                        } else {
                            "Add your first expense to see it here"
                        }
                    )
                }
            } else {
                item {
                    ModernSectionHeader(
                        title = "${uiState.expenses.size} transactions"
                    )
                }
                items(
                    items = uiState.expenses,
                    key = { expense -> expense.id }
                ) { expense ->
                    ModernListItemCard(
                        avatarIcon = Icons.Default.Receipt,
                        avatarColor = when (expense.syncStatus) {
                            ExpenseSyncStatus.SYNCED -> com.smartexpense.designsystems.ModernSuccessColor
                            ExpenseSyncStatus.FAILED -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        },
                        title = expense.merchant.ifBlank {
                            expense.description.ifBlank { "Transaction" }
                        },
                        subtitle = categoryName(expense.categoryId, uiState.categories) +
                            " • " + expense.date.toDateString(),
                        trailing = {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = expense.amount.toPesoAmount(),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (expense.syncStatus == ExpenseSyncStatus.FAILED) {
                                    TextButton(
                                        onClick = {
                                            viewModel.onIntent(
                                                TransactionIntent.RetrySync(expense.id)
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                    ) {
                                        Text("Retry", fontSize = 10.sp)
                                    }
                                }
                            }
                        },
                        onClick = { onExpenseClick(expense.id) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Filter Dialog
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = {
                Text("Filter Transactions", fontWeight = FontWeight.Bold)
            },
            text = {
                FilterDialogContent(
                    uiState = uiState,
                    categories = uiState.categories,
                    showStartDatePicker = showStartDatePicker,
                    showEndDatePicker = showEndDatePicker,
                    onShowStartDatePicker = { showStartDatePicker = true },
                    onShowEndDatePicker = { showEndDatePicker = true },
                    onIntent = viewModel::onIntent,
                    sortOrderOptions = sortOrderOptions
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.onIntent(TransactionIntent.ApplyFilters)
                    showFilterDialog = false
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.onIntent(TransactionIntent.CancelFilters)
                    showFilterDialog = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onIntent(
                        TransactionIntent.TempDateRangeChanged(
                            startTime = startDatePickerState.selectedDateMillis,
                            endTime = uiState.filter.tempEndTime
                        )
                    )
                    showStartDatePicker = false
                }) { Text("Select") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = startDatePickerState)
        }
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onIntent(
                        TransactionIntent.TempDateRangeChanged(
                            startTime = uiState.filter.tempStartTime,
                            endTime = endDatePickerState.selectedDateMillis
                        )
                    )
                    showEndDatePicker = false
                }) { Text("Select") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = endDatePickerState)
        }
    }
}

@Composable
private fun FilterDialogContent(
    uiState: TransactionUiState,
    categories: List<Category>,
    showStartDatePicker: Boolean,
    showEndDatePicker: Boolean,
    onShowStartDatePicker: () -> Unit,
    onShowEndDatePicker: () -> Unit,
    onIntent: (TransactionIntent) -> Unit,
    sortOrderOptions: List<Pair<SortOrder, String>>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Category filter
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Category", fontWeight = FontWeight.SemiBold)
            if (categories.isEmpty()) {
                Text("No categories available", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                categories.forEach { category ->
                    val isSelected = uiState.filter.tempSelectedCategoryIds.contains(category.id)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                onIntent(
                                    if (checked) TransactionIntent.TempCategorySelectionChanged(
                                        uiState.filter.tempSelectedCategoryIds + category.id
                                    ) else TransactionIntent.TempCategorySelectionChanged(
                                        uiState.filter.tempSelectedCategoryIds - category.id
                                    )
                                )
                            }
                        )
                        Text(category.name, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Date range filter
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Date Range", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onShowStartDatePicker,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(uiState.filter.tempStartTime?.toDateString() ?: "Start Date")
                }
                OutlinedButton(
                    onClick = onShowEndDatePicker,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(uiState.filter.tempEndTime?.toDateString() ?: "End Date")
                }
            }
        }

        // Amount range
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Amount Range", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uiState.filter.tempMinAmount?.toString() ?: "",
                    onValueChange = {
                        val d = it.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
                        onIntent(TransactionIntent.TempAmountRangeChanged(d, uiState.filter.tempMaxAmount))
                    },
                    label = { Text("Min") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = uiState.filter.tempMaxAmount?.toString() ?: "",
                    onValueChange = {
                        val d = it.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
                        onIntent(TransactionIntent.TempAmountRangeChanged(uiState.filter.tempMinAmount, d))
                    },
                    label = { Text("Max") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Sort order
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Sort By", fontWeight = FontWeight.SemiBold)
            sortOrderOptions.forEach { (option, label) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = uiState.filter.tempSortOrder == option,
                        onClick = { onIntent(TransactionIntent.TempSortChanged(option)) }
                    )
                    Text(label, modifier = Modifier.weight(1f))
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
