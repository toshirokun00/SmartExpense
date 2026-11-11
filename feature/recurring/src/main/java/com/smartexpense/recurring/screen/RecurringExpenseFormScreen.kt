package com.smartexpense.recurring.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpense.core.common.toDateString
import com.smartexpense.designsystems.ModernHeader
import com.smartexpense.domain.model.RecurrenceFrequency
import com.smartexpense.recurring.effect.RecurringExpenseFormEffect
import com.smartexpense.recurring.intent.RecurringExpenseFormIntent
import com.smartexpense.recurring.viewmodel.RecurringExpenseFormViewModel
import org.koin.androidx.compose.koinViewModel

private val RecurringSectionCardShape = RoundedCornerShape(24.dp)
private val RecurringFormFieldShape = RoundedCornerShape(14.dp)

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

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showFrequencyDropdown by remember { mutableStateOf(false) }

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
        // The header draws behind the status bar and handles its own inset.
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding()
        ) {
            ModernHeader(
                title = if (form.editingScheduleId == null) {
                    "Add Recurring Expense"
                } else {
                    "Edit Recurring Expense"
                },
                subtitle = "Set up automated expense tracking",
                onBackClick = onNavigateBack
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Expense Details section
                    RecurringSection(
                        title = "Expense Details",
                        accent = MaterialTheme.colorScheme.primary
                    ) {
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
                            colors = expenseFieldColors(
                                MaterialTheme.colorScheme.tertiary
                            ),
                            shape = RecurringFormFieldShape
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
                                colors = expenseFieldColors(
                                    MaterialTheme.colorScheme.secondary
                                ),
                                shape = RecurringFormFieldShape,
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
                            colors = expenseFieldColors(
                                MaterialTheme.colorScheme.primary
                            ),
                            shape = RecurringFormFieldShape
                        )
                    }

                    // Schedule Details section
                    RecurringSection(
                        title = "Schedule Details",
                        accent = MaterialTheme.colorScheme.secondary
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
                                colors = expenseFieldColors(
                                    MaterialTheme.colorScheme.tertiary
                                ),
                                shape = RecurringFormFieldShape,
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
                                colors = expenseFieldColors(
                                    MaterialTheme.colorScheme.primary
                                ),
                                shape = RecurringFormFieldShape,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = form.frequency.intervalUnitLabel(
                                    form.interval
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedTextField(
                            value = form.startDate?.toDateString() ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text("Start Date")
                            },
                            colors = expenseFieldColors(
                                MaterialTheme.colorScheme.primary
                            ),
                            shape = RecurringFormFieldShape,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { showStartDatePicker = true }) {
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
                            colors = expenseFieldColors(
                                MaterialTheme.colorScheme.secondary
                            ),
                            shape = RecurringFormFieldShape,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                Row {
                                    if (form.endDate != null) {
                                        IconButton(onClick = {
                                            viewModel.onIntent(
                                                RecurringExpenseFormIntent
                                                    .EndDateChanged(null)
                                            )
                                        }) {
                                        }
                                    }
                                    IconButton(onClick = { showEndDatePicker = true }) {
                                    }
                                }
                            }
                        )
                    }

                    // Action button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape = RoundedCornerShape(18.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.secondary,
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                                    )
                                )
                            )
                            .clickable(
                                enabled = !form.isSaving && !form.isLoading,
                                onClick = {
                                    viewModel.onIntent(RecurringExpenseFormIntent.Save)
                                }
                            )
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (form.isSaving || form.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = if (form.editingScheduleId == null) {
                                    "Create Schedule"
                                } else {
                                    "Save Changes"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
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
                TextButton(onClick = { showStartDatePicker = false }) {
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
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ───────────────────────────────────────────────────────────────────────
// Lively colour touches for the Recurring Expense form
// ───────────────────────────────────────────────────────────────────────

@Composable
private fun RecurringSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Update,
    accent: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RecurringSectionCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = accent.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = accent
                        )
                    }
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            content()
        }
    }
}

@Composable
private fun expenseFieldColors(accent: Color): TextFieldColors =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = accent,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedLabelColor = accent,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = accent,
        focusedContainerColor = accent.copy(alpha = 0.05f),
        unfocusedContainerColor = Color.Transparent
    )

private fun RecurrenceFrequency.label(): String {
    return when (this) {
        RecurrenceFrequency.WEEKLY -> "Weekly"
        RecurrenceFrequency.MONTHLY -> "Monthly"
        RecurrenceFrequency.YEARLY -> "Yearly"
        else -> "Unknown"
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

        else -> "Unknown"
    }
}
