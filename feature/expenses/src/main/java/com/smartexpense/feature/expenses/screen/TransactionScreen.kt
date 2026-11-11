package com.smartexpense.feature.expenses.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpense.core.common.toDateString
import com.smartexpense.core.common.toPesoAmount
import com.smartexpense.designsystems.LivelyBlue
import com.smartexpense.designsystems.LivelyChoicePill
import com.smartexpense.designsystems.LivelyCyan
import com.smartexpense.designsystems.LivelyGradientButton
import com.smartexpense.designsystems.LivelyGreen
import com.smartexpense.designsystems.LivelyIconBadge
import com.smartexpense.designsystems.LivelyIndigo
import com.smartexpense.designsystems.LivelyRose
import com.smartexpense.designsystems.LivelySectionCard
import com.smartexpense.designsystems.LivelyViolet
import com.smartexpense.designsystems.ModernEmptyState
import com.smartexpense.designsystems.ModernHeader
import com.smartexpense.designsystems.ModernSectionHeader
import com.smartexpense.domain.model.Category
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.model.ExpenseSyncStatus
import com.smartexpense.domain.model.SortOrder
import com.smartexpense.feature.expenses.ui.DateRangeCalendarDialog
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

    var showFilterDialog by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        ModernHeader(
            title = "Transactions",
            subtitle = "Search and filter your expenses",
            icon = Icons.Default.Receipt,
            onBackClick = onNavigateBack,
            actions = {
                TextButton(
                    onClick = { showFilterDialog = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filters",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Filter",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )

        // Active filter chip
        if (uiState.query.isNotEmpty() || uiState.selectedCategoryIds.isNotEmpty() ||
            uiState.startTime != null || uiState.endTime != null ||
            uiState.sortOrder != SortOrder.NEWEST
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = LivelyIndigo.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(percent = 50),
                    border = BorderStroke(1.dp, LivelyIndigo.copy(alpha = 0.28f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = LivelyIndigo
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Active filters",
                            style = MaterialTheme.typography.labelSmall,
                            color = LivelyIndigo,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                TextButton(
                    onClick = { viewModel.onIntent(TransactionIntent.ClearFilters) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = LivelyRose
                    )
                ) {
                    Text("Clear", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = LivelyIndigo
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LivelyIndigo,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedLabelColor = LivelyIndigo,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = LivelyIndigo,
                focusedContainerColor = LivelyIndigo.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = LivelyIndigo)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Loading transactions...", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    ModernSectionHeader(title = "${uiState.expenses.size} transactions")
                }
                items(
                    items = uiState.expenses,
                    key = { expense -> expense.id }
                ) { expense ->
                    TransactionCard(
                        expense = expense,
                        categoryName = categoryName(expense.categoryId, uiState.categories),
                        onRetrySync = { id ->
                            viewModel.onIntent(TransactionIntent.RetrySync(id))
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
                Text("Filter Transactions", fontWeight = FontWeight.Bold, color = LivelyIndigo)
            },
            text = {
                FilterDialogContent(
                    uiState = uiState,
                    categories = uiState.categories,
                    onShowDateRangePicker = { showDateRangePicker = true },
                    onIntent = viewModel::onIntent,
                    sortOrderOptions = sortOrderOptions
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onIntent(TransactionIntent.ApplyFilters)
                        showFilterDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LivelyIndigo,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(percent = 50)
                ) {
                    Text("Apply", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.onIntent(TransactionIntent.CancelFilters)
                        showFilterDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = LivelyRose)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showDateRangePicker) {
        DateRangeCalendarDialog(
            initialStartDateInclusive = uiState.filter.tempStartTime ?: 0L,
            initialEndDateExclusive = (uiState.filter.tempEndTime ?: 0L).let {
                if (it == 0L) 0L else it + 86400000L
            },
            onDismiss = { showDateRangePicker = false },
            onRangeSelected = { start, end ->
                viewModel.onIntent(
                    TransactionIntent.TempDateRangeChanged(
                        startTime = start,
                        endTime = end - 86400000L
                    )
                )
                showDateRangePicker = false
            }
        )
    }
}

@Composable
private fun TransactionCard(
    expense: Expense,
    categoryName: String,
    onRetrySync: (Long) -> Unit,
    onClick: () -> Unit
) {
    val syncColor = when (expense.syncStatus) {
        ExpenseSyncStatus.SYNCED -> LivelyGreen
        ExpenseSyncStatus.FAILED -> LivelyRose
        else -> LivelyIndigo
    }

    LivelySectionCard(
        accent = LivelyIndigo,
        accentEnd = LivelyViolet,
        modifier = Modifier.clickable(onClick = onClick),
        compact = true
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LivelyIconBadge(
                icon = Icons.Default.Receipt,
                accent = syncColor,
                accentEnd = syncColor,
                size = 34.dp,
                shape = RoundedCornerShape(12.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.merchant.ifBlank {
                        expense.description.ifBlank { "Transaction" }
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = "$categoryName • ${expense.date.toDateString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (expense.syncStatus == ExpenseSyncStatus.FAILED) {
                    Text(
                        text = "Sync failed",
                        style = MaterialTheme.typography.labelSmall,
                        color = LivelyRose,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = expense.amount.toPesoAmount(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (expense.syncStatus == ExpenseSyncStatus.FAILED) {
                    TextButton(
                        onClick = { onRetrySync(expense.id) },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text("Retry", fontSize = 10.sp, color = LivelyRose)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterDialogContent(
    uiState: TransactionUiState,
    categories: List<Category>,
    onShowDateRangePicker: () -> Unit,
    onIntent: (TransactionIntent) -> Unit,
    sortOrderOptions: List<Pair<SortOrder, String>>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Category filter
        LivelySectionCard(
            accent = LivelyIndigo,
            accentEnd = LivelyViolet,
            title = "Category",
            icon = Icons.Default.Category
        ) {
            if (categories.isEmpty()) {
                Text(
                    "No categories available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                categories.forEach { category ->
                    val isSelected = uiState.filter.tempSelectedCategoryIds.contains(category.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
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
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = LivelyIndigo,
                                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text(category.name, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Date range
        LivelySectionCard(
            accent = LivelyBlue,
            accentEnd = LivelyCyan,
            title = "Date Range",
            subtitle = if (uiState.filter.tempStartTime != null && uiState.filter.tempEndTime != null) {
                "${uiState.filter.tempStartTime!!.toDateString()} – ${uiState.filter.tempEndTime!!.toDateString()}"
            } else {
                "Tap to choose a range"
            },
            icon = Icons.Default.DateRange
        ) {
            Surface(
                onClick = onShowDateRangePicker,
                shape = RoundedCornerShape(percent = 50),
                color = LivelyIndigo.copy(alpha = 0.12f),
                contentColor = LivelyIndigo
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Select range",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Amount range
        LivelySectionCard(
            accent = LivelyIndigo,
            accentEnd = LivelyViolet,
            title = "Amount Range",
            icon = Icons.Default.Receipt
        ) {
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LivelyIndigo,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedLabelColor = LivelyIndigo,
                        cursorColor = LivelyIndigo,
                        focusedContainerColor = LivelyIndigo.copy(alpha = 0.05f)
                    ),
                    shape = RoundedCornerShape(14.dp)
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LivelyIndigo,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedLabelColor = LivelyIndigo,
                        cursorColor = LivelyIndigo,
                        focusedContainerColor = LivelyIndigo.copy(alpha = 0.05f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }

        // Sort order
        LivelySectionCard(
            accent = LivelyIndigo,
            accentEnd = LivelyViolet,
            title = "Sort By",
            icon = Icons.Default.FilterList
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sortOrderOptions.forEach { (option, label) ->
                    LivelyChoicePill(
                        label = label,
                        selected = uiState.filter.tempSortOrder == option,
                        accent = LivelyIndigo,
                        accentEnd = LivelyViolet,
                        role = androidx.compose.ui.semantics.Role.RadioButton,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onIntent(TransactionIntent.TempSortChanged(option))
                        }
                    )
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