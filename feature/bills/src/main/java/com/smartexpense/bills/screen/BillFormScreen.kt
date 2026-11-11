package com.smartexpense.bills.screen

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpense.bills.effect.BillFormEffect
import com.smartexpense.bills.intent.BillFormIntent
import com.smartexpense.bills.util.REMINDER_DAY_OPTIONS
import com.smartexpense.bills.util.formatReminderTime
import com.smartexpense.bills.util.intervalUnitLabel
import com.smartexpense.bills.util.reminderDaysLabel
import com.smartexpense.bills.viewmodel.BillFormViewModel
import com.smartexpense.designsystems.LivelyBlue
import com.smartexpense.designsystems.LivelyIndigo
import com.smartexpense.designsystems.LivelyRose
import com.smartexpense.designsystems.LivelySectionCard
import com.smartexpense.designsystems.LivelyToggleRow
import com.smartexpense.designsystems.LivelyViolet
import com.smartexpense.designsystems.ModernHeader
import com.smartexpense.designsystems.MoneyAmountField
import com.smartexpense.domain.model.BillAmountType
import com.smartexpense.domain.model.BillFrequency
import com.smartexpense.domain.model.PaymentMethod
import com.smartexpense.feature.expenses.ui.ExpenseDateField
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillFormScreen(
    viewModel: BillFormViewModel = koinViewModel(),
    billId: Long? = null,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val form = uiState.form
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var amountTypeExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var billTypeExpanded by remember { mutableStateOf(false) }
    var frequencyExpanded by remember { mutableStateOf(false) }
    var paymentMethodExpanded by remember { mutableStateOf(false) }
    var remindersExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(billId) {
        viewModel.load(billId)
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                BillFormEffect.Saved,
                BillFormEffect.Deleted,
                BillFormEffect.Skipped -> onSaved()

                is BillFormEffect.ShowError ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    if (showTimePicker) {
        LaunchedEffect(showTimePicker) {
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    viewModel.onIntent(
                        BillFormIntent.ReminderTimeChanged(hour * 60 + minute)
                    )
                    showTimePicker = false
                },
                form.reminderTimeMinutes / 60,
                form.reminderTimeMinutes % 60,
                false
            ).apply {
                setOnCancelListener { showTimePicker = false }
            }.show()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                title = if (form.editingBillId == null) "Add Bill" else "Edit Bill",
                subtitle = "Track due dates, payments, and reminders",
                icon = Icons.Default.Receipt,
                onBackClick = onNavigateBack
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        LivelySectionCard(
                            accent = LivelyIndigo,
                            accentEnd = LivelyViolet,
                            title = "Bill details",
                            icon = Icons.Default.Receipt
                        ) {
                            OutlinedTextField(
                                value = form.payee,
                                onValueChange = {
                                    viewModel.onIntent(BillFormIntent.PayeeChanged(it))
                                },
                                label = { Text("Payee / biller") },
                                placeholder = { Text("e.g. Meralco") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MoneyAmountField(
                                    value = form.amount,
                                    onValueChange = {
                                        viewModel.onIntent(BillFormIntent.AmountChanged(it))
                                    },
                                    label = "Bill amount",
                                    prefix = "₱",
                                    modifier = Modifier.weight(1f)
                                )
                                BillDropdownField(
                                    label = "Type",
                                    value = if (form.amountType == BillAmountType.VARIABLE) {
                                        "Variable"
                                    } else {
                                        "Fixed"
                                    },
                                    expanded = amountTypeExpanded,
                                    onExpandedChange = { amountTypeExpanded = it },
                                    accent = LivelyBlue,
                                    modifier = Modifier.width(140.dp)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Fixed") },
                                        trailingIcon = {
                                            if (form.amountType == BillAmountType.STATIC) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = LivelyBlue
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.onIntent(
                                                BillFormIntent.AmountTypeChanged(
                                                    BillAmountType.STATIC
                                                )
                                            )
                                            amountTypeExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Variable") },
                                        trailingIcon = {
                                            if (form.amountType == BillAmountType.VARIABLE) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = LivelyBlue
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.onIntent(
                                                BillFormIntent.AmountTypeChanged(
                                                    BillAmountType.VARIABLE
                                                )
                                            )
                                            amountTypeExpanded = false
                                        }
                                    )
                                }
                            }
                            if (form.amountType == BillAmountType.VARIABLE) {
                                Text(
                                    "Enter your best estimate. When you mark it paid you " +
                                        "can enter the actual amount — future estimates use " +
                                        "your recent payments.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            ExpenseDateField(
                                dateMillis = form.dueDateMillis,
                                onDateSelected = {
                                    viewModel.onIntent(BillFormIntent.DueDateChanged(it))
                                },
                                label = "Due date",
                                accentColor = LivelyIndigo
                            )

                            BillDropdownField(
                                label = "Category",
                                value = uiState.categories
                                    .firstOrNull { it.id == form.categoryId }
                                    ?.name
                                    ?: "None",
                                expanded = categoryExpanded,
                                onExpandedChange = { categoryExpanded = it },
                                accent = LivelyIndigo
                            ) {
                                DropdownMenuItem(
                                    text = { Text("None") },
                                    trailingIcon = {
                                        if (form.categoryId == null) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = LivelyIndigo
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.onIntent(
                                            BillFormIntent.CategoryChanged(null)
                                        )
                                        categoryExpanded = false
                                    }
                                )
                                uiState.categories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category.name) },
                                        trailingIcon = {
                                            if (form.categoryId == category.id) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = LivelyIndigo
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.onIntent(
                                                BillFormIntent.CategoryChanged(category.id)
                                            )
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                            if (uiState.categories.isEmpty()) {
                                Text(
                                    "No categories yet — you can add one under Categories.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            OutlinedTextField(
                                value = form.notes,
                                onValueChange = {
                                    viewModel.onIntent(BillFormIntent.NotesChanged(it))
                                },
                                label = { Text("Notes (account number, portal, etc.)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }
                    }

                    item {
                        LivelySectionCard(
                            accent = LivelyIndigo,
                            accentEnd = LivelyViolet,
                            title = "Schedule",
                            icon = Icons.Default.Event
                        ) {
                            BillDropdownField(
                                label = "Bill type",
                                value = if (form.isRecurring) "Recurring" else "One-time",
                                expanded = billTypeExpanded,
                                onExpandedChange = { billTypeExpanded = it },
                                accent = LivelyIndigo
                            ) {
                                DropdownMenuItem(
                                    text = { Text("One-time") },
                                    trailingIcon = {
                                        if (!form.isRecurring) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = LivelyIndigo
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.onIntent(BillFormIntent.RecurringChanged(false))
                                        billTypeExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Recurring") },
                                    trailingIcon = {
                                        if (form.isRecurring) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = LivelyIndigo
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.onIntent(BillFormIntent.RecurringChanged(true))
                                        billTypeExpanded = false
                                    }
                                )
                            }
                            if (form.isRecurring) {
                                val frequencies = listOf(
                                    BillFrequency.WEEKLY to "Weekly",
                                    BillFrequency.MONTHLY to "Monthly",
                                    BillFrequency.QUARTERLY to "Quarterly",
                                    BillFrequency.YEARLY to "Yearly"
                                )
                                BillDropdownField(
                                    label = "Repeat every",
                                    value = frequencies
                                        .firstOrNull { it.first == form.frequency }
                                        ?.second
                                        ?: "Monthly",
                                    expanded = frequencyExpanded,
                                    onExpandedChange = { frequencyExpanded = it },
                                    accent = LivelyBlue
                                ) {
                                    frequencies.forEach { (frequency, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            trailingIcon = {
                                                if (form.frequency == frequency) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = LivelyBlue
                                                    )
                                                }
                                            },
                                            onClick = {
                                                viewModel.onIntent(
                                                    BillFormIntent.FrequencyChanged(frequency)
                                                )
                                                frequencyExpanded = false
                                            }
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = form.intervalText,
                                        onValueChange = {
                                            viewModel.onIntent(BillFormIntent.IntervalChanged(it))
                                        },
                                        label = { Text("Interval") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number
                                        ),
                                        modifier = Modifier.width(110.dp),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    Text(
                                        form.frequency.intervalUnitLabel(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    item {
                        LivelySectionCard(
                            accent = LivelyBlue,
                            accentEnd = LivelyViolet,
                            title = "Payment",
                            icon = Icons.Default.CreditCard
                        ) {
                            BillDropdownField(
                                label = "Payment method",
                                value = form.paymentMethod.displayName,
                                expanded = paymentMethodExpanded,
                                onExpandedChange = { paymentMethodExpanded = it },
                                accent = LivelyBlue
                            ) {
                                PaymentMethod.entries.toList().forEach { method ->
                                    DropdownMenuItem(
                                        text = { Text(method.displayName) },
                                        trailingIcon = {
                                            if (form.paymentMethod == method) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = LivelyBlue
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.onIntent(
                                                BillFormIntent.PaymentMethodChanged(method)
                                            )
                                            paymentMethodExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        LivelySectionCard(
                            accent = LivelyIndigo,
                            accentEnd = LivelyBlue,
                            title = "Reminders & status",
                            icon = Icons.Default.NotificationsActive
                        ) {
                            BillDropdownField(
                                label = "Reminders",
                                value = reminderDaysLabel(form.reminderDays),
                                expanded = remindersExpanded,
                                onExpandedChange = { remindersExpanded = it },
                                accent = LivelyIndigo
                            ) {
                                REMINDER_DAY_OPTIONS.forEach { days ->
                                    val selected = form.reminderDays.contains(days)
                                    DropdownMenuItem(
                                        text = { Text(reminderDaysLabel(days)) },
                                        trailingIcon = {
                                            if (selected) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = LivelyIndigo
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.onIntent(
                                                BillFormIntent.ReminderDaysToggled(days)
                                            )
                                        }
                                    )
                                }
                            }
                            Text(
                                "Select all that apply. You'll be notified on each chosen day " +
                                    "before the bill is due.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = { showTimePicker = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    Icons.Default.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Reminder time: " +
                                        formatReminderTime(form.reminderTimeMinutes)
                                )
                            }
                            LivelyToggleRow(
                                title = "Auto-pay",
                                subtitle = "Mark this bill as auto-debited",
                                icon = Icons.Default.CreditCard,
                                accent = LivelyBlue,
                                checked = form.autopay,
                                onCheckedChange = {
                                    viewModel.onIntent(BillFormIntent.AutopayChanged(it))
                                }
                            )
                            LivelyToggleRow(
                                title = "Bill is active",
                                subtitle = "Paused bills are hidden from due reminders",
                                icon = Icons.Default.NotificationsActive,
                                accent = LivelyIndigo,
                                checked = form.isActive,
                                onCheckedChange = {
                                    viewModel.onIntent(BillFormIntent.ActiveChanged(it))
                                }
                            )
                        }
                    }

                    item {
                        Button(
                            onClick = { viewModel.onIntent(BillFormIntent.Save) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            enabled = !form.isSaving,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LivelyIndigo,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(percent = 50)
                        ) {
                            if (form.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Text(
                                    if (form.editingBillId == null) "Save Bill" else "Update Bill",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (form.editingBillId != null) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (form.canSkip) {
                                    TextButton(
                                        onClick = { viewModel.onIntent(BillFormIntent.Skip) },
                                        modifier = Modifier.weight(1f),
                                        enabled = !form.isSaving,
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = LivelyBlue
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.SkipNext,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("Skip", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                TextButton(
                                    onClick = { confirmDelete = true },
                                    modifier = Modifier.weight(1f),
                                    enabled = !form.isSaving,
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = LivelyRose
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Delete", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = {
                Text(
                    "Delete bill?",
                    fontWeight = FontWeight.Bold,
                    color = LivelyRose
                )
            },
            text = { Text("This bill and its payment history will be permanently removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        viewModel.onIntent(BillFormIntent.Delete)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LivelyRose,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(percent = 50)
                ) {
                    Text("Delete", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmDelete = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = LivelyIndigo)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BillDropdownField(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                focusedLabelColor = accent,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = accent
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            matchAnchorWidth = true,
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
            shadowElevation = 8.dp
        ) {
            content()
        }
    }
}

