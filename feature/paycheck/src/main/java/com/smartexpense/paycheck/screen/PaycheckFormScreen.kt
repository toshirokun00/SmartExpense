package com.smartexpense.paycheck.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpense.core.common.toDateString
import com.smartexpense.core.common.toPesoAmount
import com.smartexpense.core.common.util.MoneyInputUtil
import com.smartexpense.designsystems.LivelyBlue
import com.smartexpense.designsystems.LivelyIndigo
import com.smartexpense.designsystems.LivelyRose
import com.smartexpense.designsystems.LivelySectionCard
import com.smartexpense.designsystems.LivelyViolet
import com.smartexpense.designsystems.ModernEmptyState
import com.smartexpense.designsystems.ModernHeader
import com.smartexpense.designsystems.ModernHeaderActionPill
import com.smartexpense.designsystems.MoneyAmountField
import com.smartexpense.domain.model.Bill
import com.smartexpense.domain.model.PaycheckFrequency
import com.smartexpense.domain.model.isOccurrencePaid
import java.time.ZoneId
import com.smartexpense.domain.model.ordinalDay
import com.smartexpense.feature.expenses.ui.ExpenseDateField
import com.smartexpense.paycheck.effect.PaycheckFormEffect
import com.smartexpense.paycheck.intent.PaycheckFormIntent
import com.smartexpense.paycheck.state.PaycheckFormState
import com.smartexpense.paycheck.state.PaycheckFormUiState
import com.smartexpense.paycheck.util.billMonthSections
import com.smartexpense.paycheck.viewmodel.PaycheckFormViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun PaycheckFormScreen(
    paycheckId: Long?,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: PaycheckFormViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmStop by remember { mutableStateOf(false) }

    LaunchedEffect(paycheckId) {
        viewModel.load(paycheckId)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                PaycheckFormEffect.Saved -> onSaved()
                PaycheckFormEffect.Deleted -> onDeleted()
                PaycheckFormEffect.RecurringStopped ->
                    snackbarHostState.showSnackbar(
                        "Recurring stopped. This paycheck is kept."
                    )
                is PaycheckFormEffect.ShowError ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ModernHeader(
                title = if (uiState.form.isEditing) "Edit Paycheck" else "New Paycheck",
                subtitle = "Set the income and choose the bills it covers",
                icon = Icons.Default.Payments,
                onBackClick = onNavigateBack,
                actions = {
                    if (uiState.form.isEditing) {
                        ModernHeaderActionPill(
                            label = "Delete",
                            icon = Icons.Default.Delete,
                            onClick = { confirmDelete = true }
                        )
                    }
                }
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                PaycheckFormContent(
                    uiState = uiState,
                    onIntent = viewModel::onIntent,
                    onStopRequested = { confirmStop = true }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = {
                Text(
                    text = if (uiState.form.isRecurringOccurrence) {
                        "Delete recurring paycheck?"
                    } else {
                        "Delete paycheck?"
                    },
                    fontWeight = FontWeight.Medium,
                    color = LivelyRose
                )
            },
            text = {
                Text(
                    if (uiState.form.isRecurringOccurrence) {
                        "This paycheck is part of a recurring schedule. " +
                            "Every paycheck created by this schedule will " +
                            "also be deleted, and the schedule will stop repeating."
                    } else {
                        "This paycheck will be permanently removed."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        viewModel.onIntent(PaycheckFormIntent.Delete)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LivelyRose,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(percent = 50)
                ) {
                    Text(
                        text = if (uiState.form.isRecurringOccurrence) {
                            "Delete all"
                        } else {
                            "Delete"
                        },
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmDelete = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = LivelyIndigo
                    )
                ) {
                    Text("Cancel", fontWeight = FontWeight.Medium)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (confirmStop) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            title = {
                Text(
                    text = "Stop repeating paycheck?",
                    fontWeight = FontWeight.Medium,
                    color = LivelyRose
                )
            },
            text = {
                Text(
                    "This paycheck will be kept. Every later paycheck of " +
                        "this schedule will be removed."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmStop = false
                        viewModel.onIntent(PaycheckFormIntent.StopRepeating)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LivelyRose,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(percent = 50)
                ) {
                    Text("Stop after this", fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmStop = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = LivelyIndigo
                    )
                ) {
                    Text("Cancel", fontWeight = FontWeight.Medium)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun PaycheckFormContent(
    uiState: PaycheckFormUiState,
    onIntent: (PaycheckFormIntent) -> Unit,
    onStopRequested: () -> Unit
) {
    val form = uiState.form
    val assignedTotal = uiState.assignedTotal()
    val amount = MoneyInputUtil.cleanAmount(form.amount).toDoubleOrNull() ?: 0.0
    val remaining = amount - assignedTotal

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MoneyAmountField(
            value = form.amount,
            onValueChange = {
                onIntent(PaycheckFormIntent.AmountChanged(it))
            },
            label = "Paycheck amount",
            prefix = "₱"
        )

        OutlinedTextField(
            value = form.label,
            onValueChange = {
                onIntent(PaycheckFormIntent.LabelChanged(it))
            },
            label = { Text("Label (optional)") },
            placeholder = { Text("e.g. 1st salary") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        if (form.canEditPayDate) {
            ExpenseDateField(
                dateMillis = form.payDateMillis,
                onDateSelected = {
                    onIntent(PaycheckFormIntent.PayDateChanged(it))
                },
                label = "Pay date",
                accentColor = LivelyIndigo
            )
        } else {
            PayDateCard(
                dateMillis = form.payDateMillis,
                isFirstOfSchedule = form.isRecurring
            )
        }

        if (!form.isEditing) {
            RecurrenceCard(
                form = form,
                onIntent = onIntent
            )
        }

        if (form.isRecurringOccurrence) {
            RecurringOccurrenceCard(
                form = form,
                onIntent = onIntent,
                onStopRequested = onStopRequested
            )
        }

        SummaryCard(
            amount = amount,
            assignedTotal = assignedTotal,
            remaining = remaining,
            billCount = form.selectedBillIds.size
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Bills to cover",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${form.selectedBillIds.size} selected",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "Bills due from your pay date up to the next paycheck are " +
                "selected for you. Bills already covered by another paycheck " +
                "are not listed here. Tap any bill to adjust the plan.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (form.isRecurring || form.isRecurringOccurrence) {
            Text(
                text = "Each bill is covered by one cutoff at a time. " +
                    "Recurring bills you check here stay covered on the " +
                    "later paychecks of this schedule; one-time bills " +
                    "belong to this cutoff only.",
                style = MaterialTheme.typography.bodySmall,
                color = LivelyViolet
            )
        }

        val assignableBills = uiState.assignableBills()
        if (assignableBills.isEmpty()) {
            ModernEmptyState(
                icon = Icons.Default.Receipt,
                title = "No bills to assign",
                subtitle = if (uiState.bills.isEmpty()) {
                    "Add bills first, then assign them to this paycheck."
                } else {
                    "Every bill is already covered by another paycheck."
                }
            )
        } else {
            val sections = billMonthSections(
                rows = assignableBills,
                zoneId = uiState.zoneId,
                payDateMillis = form.payDateMillis,
                dueDateMillis = uiState::displayedDueMillis,
                amount = { bill -> bill.amount }
            )
            sections.forEach { section ->
                BillMonthHeader(
                    label = section.label,
                    count = section.rows.size,
                    total = section.total
                )
                section.rows.forEach { bill ->
                    BillSelectionRow(
                        bill = bill,
                        dueDateMillis = uiState.displayedDueMillis(bill),
                        selected = bill.id in form.selectedBillIds,
                        suggested = bill.id in uiState.suggestedBillIds,
                        zoneId = uiState.zoneId,
                        onToggle = {
                            onIntent(PaycheckFormIntent.BillToggled(bill.id))
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = { onIntent(PaycheckFormIntent.Save) },
            enabled = !form.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(percent = 50)
        ) {
            if (form.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when {
                        form.isEditing -> "Save changes"
                        form.isRecurring -> "Create recurring paycheck"
                        else -> "Add paycheck"
                    },
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PayDateCard(
    dateMillis: Long,
    isFirstOfSchedule: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LivelyIndigo.copy(alpha = 0.08f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = null,
            tint = LivelyIndigo
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = if (isFirstOfSchedule) "First paycheck" else "Pay date",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = dateMillis.toDateString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun RecurrenceCard(
    form: PaycheckFormState,
    onIntent: (PaycheckFormIntent) -> Unit
) {
    LivelySectionCard(
        accent = LivelyIndigo,
        accentEnd = LivelyViolet,
        title = "Repeating paycheck",
        icon = Icons.Default.Repeat
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Repeat this paycheck",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Create each cutoff automatically",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = form.isRecurring,
                onCheckedChange = {
                    onIntent(PaycheckFormIntent.RecurringChanged(it))
                }
            )
        }

        if (form.isRecurring) {
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaycheckFrequency.entries.forEach { frequency ->
                    FilterChip(
                        selected = form.frequency == frequency,
                        onClick = {
                            onIntent(PaycheckFormIntent.FrequencyChanged(frequency))
                        },
                        label = { Text(frequency.label()) }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            when (form.frequency) {
                PaycheckFrequency.MONTHLY -> DayPicker(
                    label = "Pay day",
                    day = form.firstDay,
                    onDaySelected = {
                        onIntent(PaycheckFormIntent.FirstDayChanged(it))
                    }
                )

                PaycheckFrequency.SEMI_MONTHLY -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DayPicker(
                        label = "1st cutoff",
                        day = form.firstDay,
                        onDaySelected = {
                            onIntent(PaycheckFormIntent.FirstDayChanged(it))
                        },
                        modifier = Modifier.weight(1f)
                    )
                    DayPicker(
                        label = "2nd cutoff",
                        day = form.secondDay,
                        onDaySelected = {
                            onIntent(PaycheckFormIntent.SecondDayChanged(it))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                PaycheckFrequency.WEEKLY,
                PaycheckFrequency.BIWEEKLY -> Unit
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Short months use their last day, so a 30th cutoff " +
                    "becomes Feb 28/29.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DayPicker(
    label: String,
    day: Int,
    onDaySelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "$label · ${ordinalDay(day)}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            (1..31).forEach { option ->
                DropdownMenuItem(
                    text = { Text(ordinalDay(option)) },
                    onClick = {
                        expanded = false
                        onDaySelected(option)
                    }
                )
            }
        }
    }
}

@Composable
private fun RecurringOccurrenceCard(
    form: PaycheckFormState,
    onIntent: (PaycheckFormIntent) -> Unit,
    onStopRequested: () -> Unit
) {
    LivelySectionCard(
        accent = LivelyViolet,
        accentEnd = LivelyIndigo,
        title = "Recurring paycheck",
        icon = Icons.Default.Repeat
    ) {
        Text(
            text = "This cutoff belongs to a recurring payroll schedule. " +
                "Changes here only affect this paycheck.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (form.scheduleStopped) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Recurring stopped. This paycheck is kept; later ones were removed.",
                style = MaterialTheme.typography.labelLarge,
                color = LivelyBlue
            )
        } else {
            TextButton(
                onClick = onStopRequested
            ) {
                Text("Stop repeating")
            }
        }
    }
}

@Composable
private fun SummaryCard(
    amount: Double,
    assignedTotal: Double,
    remaining: Double,
    billCount: Int
) {
    val isShort = remaining < 0
    val accent = if (isShort) LivelyRose else LivelyIndigo

    LivelySectionCard(
        accent = accent,
        accentEnd = LivelyViolet,
        title = "Plan summary",
        icon = Icons.Default.CalendarMonth
    ) {
        SummaryRow("Paycheck", amount.toPesoAmount())
        SummaryRow(
            label = if (billCount == 1) "1 bill covered" else "$billCount bills covered",
            value = assignedTotal.toPesoAmount()
        )
        SummaryRow(
            label = if (isShort) "Short by" else "Left after bills",
            value = (if (isShort) -remaining else remaining).toPesoAmount(),
            valueColor = accent
        )
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

@Composable
private fun BillSelectionRow(
    bill: Bill,
    dueDateMillis: Long,
    selected: Boolean,
    suggested: Boolean,
    zoneId: ZoneId,
    onToggle: () -> Unit
) {
    val accent = when {
        selected -> LivelyIndigo
        suggested -> LivelyBlue
        else -> MaterialTheme.colorScheme.outline
    }

    val cyclePaid = selected && isOccurrencePaid(bill, dueDateMillis, zoneId)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = if (selected) 0.10f else 0.04f))
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(checkedColor = LivelyIndigo)
        )

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = bill.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (selected || suggested) {
                    Spacer(Modifier.width(6.dp))
                    SelectionChip(
                        label = when {
                            cyclePaid -> "Paid"
                            selected -> "Covered"
                            bill.isPaid -> "Paid"
                            else -> "Planned"
                        },
                        color = accent
                    )
                }
            }
            Text(
                text = "Due ${dueDateMillis.toDateString()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = bill.amount.toPesoAmount(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun SelectionChip(label: String, color: Color) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = color,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}
