package com.smartexpense.bills.screen

import android.app.TimePickerDialog
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
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
import com.smartexpense.designsystems.LivelyChoicePill
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
                                value = form.name,
                                onValueChange = {
                                    viewModel.onIntent(BillFormIntent.NameChanged(it))
                                },
                                label = { Text("Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )
                            MoneyAmountField(
                                value = form.amount,
                                onValueChange = {
                                    viewModel.onIntent(BillFormIntent.AmountChanged(it))
                                },
                                label = if (form.amountType == BillAmountType.VARIABLE) {
                                    "Estimated amount"
                                } else {
                                    "Amount"
                                },
                                prefix = "₱"
                            )
                            ExpenseDateField(
                                dateMillis = form.dueDateMillis,
                                onDateSelected = {
                                    viewModel.onIntent(BillFormIntent.DueDateChanged(it))
                                },
                                accentColor = LivelyIndigo
                            )

                            FieldLabel("Category")
                            if (uiState.categories.isEmpty()) {
                                Text(
                                    "No categories yet — you can add one under Categories.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LivelyChoicePill(
                                        label = "None",
                                        selected = form.categoryId == null,
                                        accent = LivelyIndigo,
                                        modifier = Modifier.fillMaxWidth(),
                                        role = Role.RadioButton,
                                        onClick = {
                                            viewModel.onIntent(
                                                BillFormIntent.CategoryChanged(null)
                                            )
                                        }
                                    )
                                    uiState.categories.forEach { category ->
                                        LivelyChoicePill(
                                            label = category.name,
                                            selected = form.categoryId == category.id,
                                            accent = LivelyIndigo,
                                            modifier = Modifier.fillMaxWidth(),
                                            role = Role.RadioButton,
                                            onClick = {
                                                viewModel.onIntent(
                                                    BillFormIntent.CategoryChanged(category.id)
                                                )
                                            }
                                        )
                                    }
                                }
                            }

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
                            FieldLabel("Bill type")
                            LivelyChoicePill(
                                label = "One-time",
                                selected = !form.isRecurring,
                                accent = LivelyIndigo,
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Default.Event,
                                role = Role.RadioButton,
                                onClick = {
                                    viewModel.onIntent(BillFormIntent.RecurringChanged(false))
                                }
                            )
                            LivelyChoicePill(
                                label = "Recurring",
                                selected = form.isRecurring,
                                accent = LivelyIndigo,
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Default.Repeat,
                                role = Role.RadioButton,
                                onClick = {
                                    viewModel.onIntent(BillFormIntent.RecurringChanged(true))
                                }
                            )
                            if (form.isRecurring) {
                                FieldLabel("Repeat every")
                                listOf(
                                    BillFrequency.WEEKLY to "Weekly",
                                    BillFrequency.MONTHLY to "Monthly",
                                    BillFrequency.QUARTERLY to "Quarterly",
                                    BillFrequency.YEARLY to "Yearly"
                                ).forEach { (frequency, label) ->
                                    LivelyChoicePill(
                                        label = label,
                                        selected = form.frequency == frequency,
                                        accent = LivelyBlue,
                                        modifier = Modifier.fillMaxWidth(),
                                        role = Role.RadioButton,
                                        onClick = {
                                            viewModel.onIntent(
                                                BillFormIntent.FrequencyChanged(frequency)
                                            )
                                        }
                                    )
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
                            title = "Amount & payment",
                            icon = Icons.Default.AttachMoney
                        ) {
                            FieldLabel("Amount type")
                            LivelyChoicePill(
                                label = "Fixed",
                                selected = form.amountType == BillAmountType.STATIC,
                                accent = LivelyBlue,
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Default.AttachMoney,
                                role = Role.RadioButton,
                                onClick = {
                                    viewModel.onIntent(
                                        BillFormIntent.AmountTypeChanged(BillAmountType.STATIC)
                                    )
                                }
                            )
                            LivelyChoicePill(
                                label = "Variable",
                                selected = form.amountType == BillAmountType.VARIABLE,
                                accent = LivelyBlue,
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Default.TrackChanges,
                                role = Role.RadioButton,
                                onClick = {
                                    viewModel.onIntent(
                                        BillFormIntent.AmountTypeChanged(
                                            BillAmountType.VARIABLE
                                        )
                                    )
                                }
                            )
                            if (form.amountType == BillAmountType.VARIABLE) {
                                Text(
                                    "Enter your best estimate. When you mark it paid you " +
                                        "can enter the actual amount — future estimates use " +
                                        "your recent payments.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            FieldLabel("Payment method")
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                PaymentMethod.entries.toList().forEach { method ->
                                    LivelyChoicePill(
                                        label = method.displayName,
                                        selected = form.paymentMethod == method,
                                        accent = LivelyBlue,
                                        modifier = Modifier.fillMaxWidth(),
                                        role = Role.RadioButton,
                                        onClick = {
                                            viewModel.onIntent(
                                                BillFormIntent.PaymentMethodChanged(method)
                                            )
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
                            FieldLabel("Remind me")
                            Text(
                                "Select all that apply. You'll be notified on each chosen day " +
                                    "before the bill is due.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                REMINDER_DAY_OPTIONS.forEach { days ->
                                    LivelyChoicePill(
                                        label = reminderDaysLabel(days),
                                        selected = form.reminderDays.contains(days),
                                        accent = LivelyIndigo,
                                        modifier = Modifier.fillMaxWidth(),
                                        role = Role.Checkbox,
                                        onClick = {
                                            viewModel.onIntent(
                                                BillFormIntent.ReminderDaysToggled(days)
                                            )
                                        }
                                    )
                                }
                            }
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

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
