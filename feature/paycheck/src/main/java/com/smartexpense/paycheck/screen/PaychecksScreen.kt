package com.smartexpense.paycheck.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpense.core.common.toDateString
import com.smartexpense.core.common.toPesoAmount
import com.smartexpense.designsystems.LivelyBlue
import com.smartexpense.designsystems.LivelyIconBadge
import com.smartexpense.designsystems.LivelyIndigo
import com.smartexpense.designsystems.LivelyProgressBar
import com.smartexpense.designsystems.LivelyRose
import com.smartexpense.designsystems.LivelySectionCard
import com.smartexpense.designsystems.LivelyViolet
import com.smartexpense.designsystems.ModernEmptyState
import com.smartexpense.designsystems.ModernHeader
import com.smartexpense.designsystems.ModernHeaderActionPill
import com.smartexpense.domain.model.Bill
import com.smartexpense.domain.model.Paycheck
import com.smartexpense.paycheck.intent.PaychecksIntent
import com.smartexpense.paycheck.state.PaychecksUiState
import com.smartexpense.paycheck.state.PlannedBill
import com.smartexpense.paycheck.util.billMonthSections
import com.smartexpense.paycheck.viewmodel.PaychecksViewModel
import org.koin.androidx.compose.koinViewModel
import java.time.Month
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun PaychecksScreen(
    viewModel: PaychecksViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onAddPaycheck: () -> Unit,
    onPaycheckClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showMonthPicker by remember { mutableStateOf(false) }
    val locale = LocalLocale.current.platformLocale

    Column(Modifier.fillMaxSize()) {
        ModernHeader(
            title = "Paychecks",
            subtitle = "Plan income and the bills it covers",
            icon = Icons.Default.Payments,
            onBackClick = onNavigateBack,
            actions = {
                ModernHeaderActionPill(
                    label = "Add Paycheck",
                    icon = Icons.Default.Add,
                    onClick = onAddPaycheck
                )
            }
        )

        MonthSelector(
            selectedMonth = uiState.selectedMonth,
            isCurrentMonth = uiState.selectedMonth == uiState.currentMonth,
            onPreviousMonth = {
                viewModel.onIntent(PaychecksIntent.PreviousMonth)
            },
            onNextMonth = {
                viewModel.onIntent(PaychecksIntent.NextMonth)
            },
            onPickMonth = { showMonthPicker = true },
            onCurrentMonth = {
                viewModel.onIntent(PaychecksIntent.CurrentMonth)
            }
        )

        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            uiState.paychecks.isEmpty() -> ModernEmptyState(
                icon = Icons.Default.Payments,
                title = emptyTitle(uiState, locale),
                subtitle = "Add a paycheck and choose the bills it should cover.",
                action = {
                    Button(onClick = onAddPaycheck) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Add Paycheck")
                    }
                }
            )

            else -> PaycheckList(
                uiState = uiState,
                onPaycheckClick = onPaycheckClick
            )
        }
    }

    if (showMonthPicker) {
        MonthPickerDialog(
            selectedMonth = uiState.selectedMonth,
            onMonthSelected = { month ->
                showMonthPicker = false
                viewModel.onIntent(
                    PaychecksIntent.SelectMonth(month.year, month.monthValue)
                )
            },
            onCurrentMonth = {
                showMonthPicker = false
                viewModel.onIntent(PaychecksIntent.CurrentMonth)
            },
            onDismiss = { showMonthPicker = false }
        )
    }
}

@Composable
private fun MonthSelector(
    selectedMonth: YearMonth,
    isCurrentMonth: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPickMonth: () -> Unit,
    onCurrentMonth: () -> Unit
) {
    val locale = LocalLocale.current.platformLocale
    val formatter = remember(locale) {
        DateTimeFormatter.ofPattern("MMMM yyyy", locale)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous month",
                    tint = LivelyIndigo
                )
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(LivelyIndigo.copy(alpha = 0.10f))
                    .clickable(onClick = onPickMonth)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedMonth.format(formatter),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = LivelyIndigo
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select month and year",
                    modifier = Modifier.size(18.dp),
                    tint = LivelyIndigo
                )
            }

            Spacer(Modifier.weight(1f))

            IconButton(onClick = onNextMonth) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next month",
                    tint = LivelyIndigo
                )
            }
        }

        if (!isCurrentMonth) {
            TextButton(onClick = onCurrentMonth) {
                Text("Back to this month")
            }
        }
    }
}

@Composable
private fun MonthPickerDialog(
    selectedMonth: YearMonth,
    onMonthSelected: (YearMonth) -> Unit,
    onCurrentMonth: () -> Unit,
    onDismiss: () -> Unit
) {
    var year by remember(selectedMonth) { mutableIntStateOf(selectedMonth.year) }
    val locale = LocalLocale.current.platformLocale

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Jump to month",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { year-- }) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous year",
                            tint = LivelyIndigo
                        )
                    }
                    Text(
                        text = year.toString(),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { year++ }) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next year",
                            tint = LivelyIndigo
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Month.values().toList().chunked(3).forEach { monthRow ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        monthRow.forEach { month ->
                            MonthOption(
                                label = month.getDisplayName(
                                    TextStyle.SHORT,
                                    locale
                                ),
                                selected = year == selectedMonth.year &&
                                    month == selectedMonth.month,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onMonthSelected(YearMonth.of(year, month))
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCurrentMonth) {
                Text("This month")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun MonthOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) LivelyIndigo else LivelyIndigo.copy(alpha = 0.08f)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else LivelyIndigo
        )
    }
}

private fun emptyTitle(uiState: PaychecksUiState, locale: Locale): String =
    if (uiState.selectedMonth == uiState.currentMonth) {
        "No paychecks this month"
    } else {
        "No paychecks in ${uiState.selectedMonth.month.getDisplayName(
            TextStyle.FULL,
            locale
        )} ${uiState.selectedMonth.year}"
    }

@Composable
private fun PaycheckList(
    uiState: PaychecksUiState,
    onPaycheckClick: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(uiState.paychecks, key = { it.id }) { paycheck ->
            PaycheckCard(
                paycheck = paycheck,
                coveredBills = uiState.plannedBills[paycheck.id].orEmpty(),
                bills = uiState.bills,
                zoneId = uiState.zoneId,
                onClick = { onPaycheckClick(paycheck.id) }
            )
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun PaycheckCard(
    paycheck: Paycheck,
    coveredBills: List<PlannedBill>,
    bills: List<Bill>,
    zoneId: ZoneId,
    onClick: () -> Unit
) {
    val billsById = bills.associateBy { it.id }
    val covered = coveredBills.mapNotNull { planned ->
        billsById[planned.billId]?.let { bill -> planned to bill }
    }
    val assigned = covered.sumOf { (_, bill) -> bill.amount }
    val allPaid = covered.isNotEmpty() && covered.all { (planned, _) -> planned.isPaid }
    val remaining = paycheck.amount - assigned
    val isShort = remaining < 0
    val accent = if (isShort) LivelyRose else LivelyIndigo

    LivelySectionCard(
        accent = accent,
        accentEnd = LivelyViolet,
        compact = true,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LivelyIconBadge(
                    icon = Icons.Default.Payments,
                    accent = accent,
                    accentEnd = LivelyViolet,
                    size = 34.dp,
                    shape = RoundedCornerShape(10.dp)
                )
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = paycheck.displayLabel(),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            ),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (paycheck.isRecurringOccurrence) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Recurring paycheck",
                                modifier = Modifier.height(13.dp),
                                tint = LivelyViolet
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.height(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = paycheck.payDateMillis.toDateString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = paycheck.amount.toPesoAmount(),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 15.sp,
                        lineHeight = 18.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            Spacer(Modifier.height(8.dp))

            val billCount = covered.size
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (billCount == 0) {
                        "No bills assigned yet"
                    } else {
                        "$billCount ${if (billCount == 1) "bill" else "bills"} • " +
                            "${assigned.toPesoAmount()} covered"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (allPaid) {
                    Text(
                        text = "All bills paid",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = LivelyBlue,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(LivelyBlue.copy(alpha = 0.14f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            if (covered.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                val sections = billMonthSections(
                    rows = covered,
                    zoneId = zoneId,
                    payDateMillis = paycheck.payDateMillis,
                    dueDateMillis = { (planned, _) -> planned.dueDateMillis },
                    amount = { (_, bill) -> bill.amount }
                )
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    sections.forEach { section ->
                        BillMonthHeader(
                            label = section.label,
                            count = section.rows.size,
                            total = section.total,
                            compact = true
                        )
                        section.rows.forEach { (planned, bill) ->
                            CoveredBillRow(
                                bill = bill,
                                dueDateMillis = planned.dueDateMillis,
                                isPaid = planned.isPaid
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            val progress = if (paycheck.amount > 0) {
                (assigned / paycheck.amount).toFloat().coerceIn(0f, 1f)
            } else {
                0f
            }
            LivelyProgressBar(
                progress = progress,
                color = accent,
                trackColor = accent.copy(alpha = 0.14f),
                barHeight = 6.dp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isShort) {
                        "Short by ${(-remaining).toPesoAmount()}"
                    } else {
                        "Left after bills"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = remaining.toPesoAmount(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = if (isShort) LivelyRose else LivelyBlue
                )
            }
        }
    }
}

/** One bill the paycheck covers, pinned to the cycle it claimed. */
@Composable
private fun CoveredBillRow(
    bill: Bill,
    dueDateMillis: Long,
    isPaid: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.06f))
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ReceiptLong,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = LivelyViolet
        )
        Text(
            text = bill.name.ifBlank { "Bill" },
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Due ${dueDateMillis.toDateString()}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            text = if (isPaid) "Paid" else "Unpaid",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isPaid) LivelyBlue else LivelyRose
        )
        Text(
            text = bill.amount.toPesoAmount(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
