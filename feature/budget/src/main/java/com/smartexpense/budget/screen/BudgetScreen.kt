package com.smartexpense.budget.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpense.budget.effect.BudgetEffect
import com.smartexpense.budget.intent.BudgetIntent
import com.smartexpense.budget.state.BudgetSummaryState
import com.smartexpense.budget.viewmodel.BudgetViewModel
import com.smartexpense.core.common.toDateString
import com.smartexpense.core.common.toPesoAmount
import com.smartexpense.designsystems.*
import com.smartexpense.domain.model.BudgetInsight
import com.smartexpense.domain.model.BudgetStatus
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            val message = when (effect) {
                is BudgetEffect.ShowError -> effect.message
                BudgetEffect.BudgetAdded -> "Budget added"
                BudgetEffect.BudgetUpdated -> "Budget updated"
                BudgetEffect.BudgetDeleted -> "Budget deleted"
            }

            snackbarHostState.showSnackbar(message)
        }
    }

    var showStartDatePicker by remember {
        mutableStateOf(false)
    }

    var showEndDatePicker by remember {
        mutableStateOf(false)
    }

    var showCategoryDropdown by remember {
        mutableStateOf(false)
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ModernHeader(
                title = if (uiState.form.editingBudgetId != null) "Edit Budget" else "Budgets",
                subtitle = "Set limits and track spending by category",
                icon = Icons.Default.Wallet,
                onBackClick = onNavigateBack
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                item {
                    ModernSectionHeader(
                        title = "Your Budgets"
                    )
                }

                items(
                    items = uiState.budgetSummaries,
                    key = {
                        it.budget.id
                    }
                ) { summary ->
                    ModernBudgetCard(
                        summary = summary,
                        onEdit = {
                            viewModel.onIntent(
                                BudgetIntent.LoadBudget(
                                    summary.budget.id
                                )
                            )
                        },
                        onDelete = {
                            viewModel.onIntent(
                                BudgetIntent.RequestDeleteBudget(
                                    summary.budget.id
                                )
                            )
                        }
                    )
                }

                if (uiState.insights.isNotEmpty()) {
                    item {
                        ModernSectionHeader(title = "Insights")
                    }
                    items(
                        items = uiState.insights,
                        key = { insight -> insight.key() }
                    ) { insight ->
                        ModernInsightCard(insight = insight)
                    }
                }

                item {
                    ModernSectionHeader(title = "New Budget")
                }

                item {
                    ModernBudgetForm(
                        uiState = uiState,
                        showCategoryDropdown = showCategoryDropdown,
                        showStartDatePicker = showStartDatePicker,
                        showEndDatePicker = showEndDatePicker,
                        onToggleCategoryDropdown = {
                            showCategoryDropdown = !showCategoryDropdown
                        },
                        onOpenStartDatePicker = {
                            showStartDatePicker = true
                        },
                        onOpenEndDatePicker = {
                            showEndDatePicker = true
                        },
                        onIntent = viewModel::onIntent
                    )
                }
            }
        }
    }

    // Start Date Picker
    if (showStartDatePicker) {

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis =
                uiState.form.startDate
        )

        DatePickerDialog(
            onDismissRequest = {
                showStartDatePicker = false
            },
            confirmButton = {

                TextButton(
                    onClick = {

                        datePickerState
                            .selectedDateMillis
                            ?.let { date ->

                                viewModel.onIntent(
                                    BudgetIntent.StartDateChanged(
                                        date
                                    )
                                )
                            }

                        showStartDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        showStartDatePicker = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState
            )
        }
    }

    // End Date Picker
    if (showEndDatePicker) {

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis =
                uiState.form.endDate
        )

        DatePickerDialog(
            onDismissRequest = {
                showEndDatePicker = false
            },
            confirmButton = {

                TextButton(
                    onClick = {

                        datePickerState
                            .selectedDateMillis
                            ?.let { date ->

                                viewModel.onIntent(
                                    BudgetIntent.EndDateChanged(
                                        date
                                    )
                                )
                            }

                        showEndDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        showEndDatePicker = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState
            )
        }
    }
    // Delete Confirmation
    if (screenState.showDeleteConfirmation) {

        AlertDialog(
            onDismissRequest = {
                viewModel.onIntent(
                    BudgetIntent.CancelDeleteBudget
                )
            },
            title = {
                Text("Delete Budget")
            },
            text = {
                Text(
                    "Are you sure you want to delete this budget?"
                )
            },
            confirmButton = {

                TextButton(
                    onClick = {
                        viewModel.onIntent(
                            BudgetIntent.ConfirmDeleteBudget
                        )
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        viewModel.onIntent(
                            BudgetIntent.CancelDeleteBudget
                        )
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ModernBudgetCard(
    summary: BudgetSummaryState,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    val progressColor = when {
        summary.isOverBudget -> ModernErrorColor
        summary.progress > 0.8f -> ModernWarningColor
        else -> ModernSuccessColor
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = progressColor.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = progressColor
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = summary.budget.startDate.toDateString() + " - " + summary.budget.endDate.toDateString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit budget",
                        tint = colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete budget",
                        tint = colorScheme.error
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Budget ${summary.budget.amount.toPesoAmount()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Spent ${summary.spentAmount.toPesoAmount()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (summary.isOverBudget) {
                        "Over by ${(-summary.remainingAmount).toPesoAmount()}"
                    } else {
                        "Remaining ${summary.remainingAmount.toPesoAmount()}"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = progressColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        LinearProgressIndicator(
            progress = { summary.progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(8.dp),
            color = progressColor,
            trackColor = progressColor.copy(alpha = 0.15f)
        )

        Text(
            text = "${summary.utilizationPercentage.toInt()}% used" +
                    summary.status.indicatorSuffix(),
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun ModernInsightCard(insight: BudgetInsight) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = insight.message(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernBudgetForm(
    uiState: com.smartexpense.budget.state.BudgetUiState,
    showCategoryDropdown: Boolean,
    showStartDatePicker: Boolean,
    showEndDatePicker: Boolean,
    onToggleCategoryDropdown: () -> Unit,
    onOpenStartDatePicker: () -> Unit,
    onOpenEndDatePicker: () -> Unit,
    onIntent: (BudgetIntent) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = showCategoryDropdown,
                onExpandedChange = {
                    onToggleCategoryDropdown()
                }
            ) {
                OutlinedTextField(
                    value = uiState.categories
                        .firstOrNull {
                            it.id == uiState.form.categoryId
                        }
                        ?.name
                        ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = {
                        Text("Category")
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = showCategoryDropdown
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = showCategoryDropdown,
                    onDismissRequest = {
                        onToggleCategoryDropdown()
                    }
                ) {
                    uiState.categories.forEach { category ->
                        DropdownMenuItem(
                            text = {
                                Text(category.name)
                            },
                            onClick = {
                                onIntent(
                                    BudgetIntent.CategoryChanged(
                                        category.id
                                    )
                                )
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = uiState.form.amount,
                onValueChange = {
                    onIntent(BudgetIntent.AmountChanged(it))
                },
                label = {
                    Text("Budget Amount")
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.form.startDate
                    ?.toDateString()
                    ?: "",
                onValueChange = {},
                readOnly = true,
                label = {
                    Text("Start Date")
                },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = onOpenStartDatePicker) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Pick start date",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )

            OutlinedTextField(
                value = uiState.form.endDate
                    ?.toDateString()
                    ?: "",
                onValueChange = {},
                readOnly = true,
                label = {
                    Text("End Date")
                },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = onOpenEndDatePicker) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Pick end date",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )

            Button(
                onClick = {
                    if (uiState.form.editingBudgetId != null) {
                        onIntent(BudgetIntent.UpdateBudget)
                    } else {
                        onIntent(BudgetIntent.AddBudget)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (
                        uiState.form.editingBudgetId != null
                    ) {
                        "Update Budget"
                    } else {
                        "Add Budget"
                    }
                )
            }

            if (uiState.form.editingBudgetId != null) {
                TextButton(
                    onClick = {
                        onIntent(BudgetIntent.CancelEdit)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel Edit")
                }
            }
        }
    }
}

private fun BudgetStatus.indicatorSuffix(): String {
    return when (this) {
        BudgetStatus.WITHIN_BUDGET -> ""
        BudgetStatus.APPROACHING_LIMIT -> " • Approaching limit"
        BudgetStatus.EXCEEDED -> " • Exceeded limit"
    }
}

private fun BudgetInsight.key(): String {
    return when (this) {
        is BudgetInsight.ApproachingLimit -> "approaching-$budgetId"
        is BudgetInsight.ExceededLimit -> "exceeded-$budgetId"
        is BudgetInsight.SpendingTrend -> "trend"
        is BudgetInsight.CategorySpendingChange -> "category-$categoryId"
    }
}

private fun BudgetInsight.message(): String {
    return when (this) {
        is BudgetInsight.ApproachingLimit ->
            "You've used ${utilizationPercentage.toInt()}% of your " +
                "$categoryName budget. ${remainingAmount.toPesoAmount()} remaining."

        is BudgetInsight.ExceededLimit ->
            "You're over your $categoryName budget by " +
                "${overAmount.toPesoAmount()} (${utilizationPercentage.toInt()}% used)."

        is BudgetInsight.SpendingTrend -> {
            val direction = if (isIncrease) "up" else "down"
            "Spending is $direction ${kotlin.math.abs(changePercentage).toInt()}% " +
                "versus the previous period " +
                "(${currentAmount.toPesoAmount()} vs ${previousAmount.toPesoAmount()})."
        }

        is BudgetInsight.CategorySpendingChange ->
            "$categoryName spending is up ${changePercentage.toInt()}% " +
                "versus the previous period " +
                "(${currentAmount.toPesoAmount()} vs ${previousAmount.toPesoAmount()})."
    }
}
