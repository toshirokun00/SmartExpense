package com.smartexpense.recurring.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (form.editingScheduleId == null) {
                            "Add Recurring Expense"
                        } else {
                            "Edit Recurring Expense"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (form.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
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
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
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
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
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

                item {
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
                                .menuAnchor()
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
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = form.frequency.intervalUnitLabel(
                                form.interval
                            )
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = form.startDate?.toDateString() ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text("Start Date")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Button(
                        onClick = {
                            showStartDatePicker = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Select Start Date")
                    }
                }

                item {
                    OutlinedTextField(
                        value = form.endDate?.toDateString() ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text("End Date (optional)")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                showEndDatePicker = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Select End Date")
                        }

                        if (form.endDate != null) {
                            TextButton(
                                onClick = {
                                    viewModel.onIntent(
                                        RecurringExpenseFormIntent
                                            .EndDateChanged(null)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Clear End Date")
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.onIntent(RecurringExpenseFormIntent.Save)
                        },
                        enabled = !form.isSaving && !form.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (form.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (form.editingScheduleId == null) {
                                    "Create Schedule"
                                } else {
                                    "Save Changes"
                                }
                            )
                        }
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
