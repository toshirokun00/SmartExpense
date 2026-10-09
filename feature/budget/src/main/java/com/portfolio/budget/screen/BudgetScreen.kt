package com.portfolio.budget.screen

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.portfolio.budget.viewmodel.BudgetViewModel
import com.portfolio.budget.intent.BudgetIntent
import com.portfolio.budget.state.BudgetScreenState
import com.portfolio.budget.state.BudgetSummaryState
import com.portfolio.budget.state.BudgetUiState
import com.smartexpense.core.common.toPesoAmount
import com.smartexpense.domain.model.Budget
import com.smartexpense.domain.model.Category
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

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
        topBar = {
            TopAppBar(
                title = {
                    Text("Budgets")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) {paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            item {
                Text(
                    text = if (uiState.form.editingBudgetId != null) {
                        "Edit Budget"
                    } else {
                        "Add Budget"
                    }
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                // Category
                ExposedDropdownMenuBox(
                    expanded = showCategoryDropdown,
                    onExpandedChange = {
                        showCategoryDropdown = !showCategoryDropdown
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
                            showCategoryDropdown = false
                        }
                    ) {
                        uiState.categories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Text(category.name)
                                },
                                onClick = {
                                    viewModel.onIntent(
                                        BudgetIntent.CategoryChanged(
                                            category.id
                                        )
                                    )

                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                // Amount
                OutlinedTextField(
                    value = uiState.form.amount,
                    onValueChange = {
                        viewModel.onIntent(
                            BudgetIntent.AmountChanged(it)
                        )
                    },
                    label = {
                        Text("Budget Amount")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                // Start Date
                OutlinedTextField(
                    value = uiState.form.startDate
                        ?.let(::formatDate)
                        ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = {
                        Text("Start Date")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Button(
                    onClick = {
                        showStartDatePicker = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select Start Date")
                }

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                // End Date
                OutlinedTextField(
                    value = uiState.form.endDate
                        ?.let(::formatDate)
                        ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = {
                        Text("End Date")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Button(
                    onClick = {
                        showEndDatePicker = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select End Date")
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Button(
                    onClick = {
                        if (uiState.form.editingBudgetId != null) {
                            viewModel.onIntent(
                                BudgetIntent.UpdateBudget
                            )
                        } else {
                            viewModel.onIntent(
                                BudgetIntent.AddBudget
                            )
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

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    TextButton(
                        onClick = {
                            viewModel.onIntent(
                                BudgetIntent.CancelEdit
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel Edit")
                    }
                }
            }

            item {
                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "Your Budgets"
                )
            }

            items(
                items = uiState.budgetSummaries,
                key = {
                    it.budget.id
                }
            ) { summary ->

                BudgetCard(
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
}

@Composable
private fun BudgetCard(
    summary: BudgetSummaryState,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = summary.categoryName
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = formatDate(
                            summary.budget.startDate
                        ) +
                                " - " +
                                formatDate(
                                    summary.budget.endDate
                                )
                    )
                }

                IconButton(
                    onClick = onEdit
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit budget"
                    )
                }

                IconButton(
                    onClick = onDelete
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete budget"
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = "Budget: ${
                    summary.budget.amount.toPesoAmount()
                }"
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "Spent: ${
                    summary.spentAmount.toPesoAmount()
                }"
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            if (summary.isOverBudget) {

                Text(
                    text = "Over budget by ${
                        (-summary.remainingAmount)
                            .toPesoAmount()
                    }"
                )

            } else {

                Text(
                    text = "Remaining: ${
                        summary.remainingAmount
                            .toPesoAmount()
                    }"
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            LinearProgressIndicator(
                progress = {
                    summary.progress
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "${(summary.progress * 100).toInt()}% used"
            )
        }
    }
}



private fun formatDate(
    timestamp: Long
): String {
    return SimpleDateFormat(
        "MMM dd, yyyy",
        Locale.getDefault()
    ).format(
        Date(timestamp)
    )
}