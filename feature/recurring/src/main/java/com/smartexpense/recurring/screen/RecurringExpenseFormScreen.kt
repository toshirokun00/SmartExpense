package com.smartexpense.recurring.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpense.core.common.toDateString
import com.smartexpense.designsystems.ModernHeader
import com.smartexpense.designsystems.ModernSectionHeader
import com.smartexpense.domain.model.RecurrenceFrequency
import com.smartexpense.recurring.effect.RecurringExpenseFormEffect
import com.smartexpense.recurring.intent.RecurringExpenseFormIntent
import com.smartexpense.recurring.viewmodel.RecurringExpenseFormViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringExpenseFormScreen(
    viewModel: RecurringExpenseFormViewModel = koinViewModel(),
    scheduleId: Long?,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val form = uiState.form

    val snackbarHostState = remember {
        SnackbarHostState()
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

    var showFrequencyDropdown by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(scheduleId) {
        if (scheduleId != null) {
            viewModel.onIntent(
                RecurringExpenseFormIntent.LoadSchedule(scheduleId)
            )
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                RecurringExpenseFormEffect.Saved -> onSaved()
                is RecurringExpenseFormEffect.ShowError ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ModernHeader(
                title = if (form.editingScheduleId == null) {
                    "Add Recurring Expense"
                } else {
                    "Edit Recurring Expense"
                },
                subtitle = "Set up automated expense tracking",
                icon = Icons.Default.Update,
                onBackClick = onNavigateBack
            )

            if (form.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                OutlinedTextField(
                                    value = form.description,
                                    onValueChange = { description ->
                                        viewModel.onIntent(
                                            RecurringExpenseFormIntent.DescriptionChanged(
                                                description
                                            )
                                        )
                                    },
                                    label = {
                                        Text("Description")
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = form.amount,
                                    onValueChange = { amount ->
                                        viewModel.onIntent(
                                            RecurringExpenseFormIntent.AmountChanged(amount)
                                        )
                                    },
                                    label = {
                                        Text("Amount")
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                ExposedDropdownMenuBox(
                                    expanded = showCategoryDropdown,
                                    onExpandedChange = {
                                        showCategoryDropdown = !showCategoryDropdown
                                    }
                                ) {
                                    OutlinedTextField(
                                        value = uiState.categories
                                            .firstOrNull { category ->
                                                category.id == form.categoryId
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
                                            .menuAnchor(),
                                        shape = RoundedCornerShape(12.dp)
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
                                                        RecurringExpenseFormIntent
                                                            .CategoryChanged(category.id)
                                                    )
                                                    showCategoryDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        ModernSectionHeader(title = "Schedule Details")
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                ExposedDropdownMenuBox(
                                    expanded = showFrequencyDropdown,
                                    onExpandedChange = {
                                        showFrequencyDropdown = !showFrequencyDropdown
                                    }
                                ) {
                                    OutlinedTextField(
                                        value = form.frequency.label(),
                                        onValueChange = {},
                                        readOnly = true,
                                        label = {
                                            Text("Frequency")
                                        },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(
                                                expanded = showFrequencyDropdown
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    ExposedDropdownMenu(
                                        expanded = showFrequencyDropdown,
                                        onDismissRequest = {
                                            showFrequencyDropdown = false
                                        }
                                    ) {
                                        RecurrenceFrequency.entries.forEach { frequency ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(frequency.label())
                                                },
                                                onClick = {
                                                    viewModel.onIntent(
                                                        RecurringExpenseFormIntent
                                                            .FrequencyChanged(frequency)
                                                    )
                                                    showFrequencyDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = form.interval,
                                        onValueChange = { interval ->
                                            viewModel.onIntent(
                                                RecurringExpenseFormIntent.IntervalChanged(
                                                    interval
                                                )
                                            )
                                        },
                                        label = {
                                            Text("Repeat every")
                                        },
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number
                                        ),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Text(
                                        text = form.frequency.intervalUnitLabel(
                                            form.interval
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                OutlinedTextField(
                                    value = form.startDate?.toDateString() ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = {
                                        Text("Start Date")
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    trailingIcon = {
                                        IconButton(onClick = { showStartDatePicker = true }) {
                                            Icon(Icons.Default.DateRange, contentDescription = null)
                                        }
                                    }
                                )

                                OutlinedTextField(
                                    value = form.endDate?.toDateString() ?: "No end date",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = {
                                        Text("End Date (optional)")
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    trailingIcon = {
                                        Row {
                                            if (form.endDate != null) {
                                                IconButton(onClick = {
                                                    viewModel.onIntent(
                                                        RecurringExpenseFormIntent
                                                            .EndDateChanged(null)
                                                    )
                                                }) {
                                                    Icon(Icons.Default.Close, contentDescription = null)
                                                }
                                            }
                                            IconButton(onClick = { showEndDatePicker = true }) {
                                                Icon(Icons.Default.DateRange, contentDescription = null)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.onIntent(RecurringExpenseFormIntent.Save)
                            },
                            enabled = !form.isSaving && !form.isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (form.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 3.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(
                                    text = if (form.editingScheduleId == null) {
                                        "Create Schedule"
                                    } else {
                                        "Save Changes"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = form.startDate
        )

        DatePickerDialog(
            onDismissRequest = {
                showStartDatePicker = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { date ->
                            viewModel.onIntent(
                                RecurringExpenseFormIntent.StartDateChanged(
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
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = form.endDate ?: form.startDate
        )

        DatePickerDialog(
            onDismissRequest = {
                showEndDatePicker = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { date ->
                            viewModel.onIntent(
                                RecurringExpenseFormIntent.EndDateChanged(
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
            DatePicker(state = datePickerState)
        }
    }
}

private fun RecurrenceFrequency.label(): String {
    return when (this) {
        RecurrenceFrequency.WEEKLY -> "Weekly"
        RecurrenceFrequency.MONTHLY -> "Monthly"
        RecurrenceFrequency.YEARLY -> "Yearly"
    }
}

private fun RecurrenceFrequency.intervalUnitLabel(
    interval: String
): String {
    val count = interval.toIntOrNull() ?: 1

    return when (this) {
        RecurrenceFrequency.WEEKLY ->
            if (count == 1) "week" else "weeks"

        RecurrenceFrequency.MONTHLY ->
            if (count == 1) "month" else "months"

        RecurrenceFrequency.YEARLY ->
            if (count == 1) "year" else "years"
    }
}
