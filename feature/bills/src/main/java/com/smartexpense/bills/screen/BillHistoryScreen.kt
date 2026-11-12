package com.smartexpense.bills.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpense.bills.state.BillHistoryUiState
import com.smartexpense.bills.state.BillPaymentMonth
import com.smartexpense.bills.util.formatHistoryCount
import com.smartexpense.bills.viewmodel.BillHistoryViewModel
import com.smartexpense.core.common.toDateString
import com.smartexpense.core.common.toPesoAmount
import com.smartexpense.designsystems.LivelyBlue
import com.smartexpense.designsystems.LivelyIconBadge
import com.smartexpense.designsystems.LivelyIndigo
import com.smartexpense.designsystems.LivelySectionCard
import com.smartexpense.designsystems.LivelyViolet
import com.smartexpense.designsystems.ModernEmptyState
import com.smartexpense.designsystems.ModernHeader
import com.smartexpense.domain.model.BillPayment
import org.koin.androidx.compose.koinViewModel
import java.time.YearMonth

/**
 * Every bill payment the user has recorded, newest month first. Totals for the
 * current and previous month sit at the top so the user can track spending on
 * bills across cycles even after recurring bills roll forward.
 */
@Composable
fun BillHistoryScreen(
    viewModel: BillHistoryViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var monthExpanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ModernHeader(
            title = "Payment history",
            subtitle = "Bills you've already paid",
            icon = Icons.Default.History,
            onBackClick = onNavigateBack
        )

        when {
            uiState.isLoading -> Unit

            uiState.isEmpty -> ModernEmptyState(
                icon = Icons.Default.History,
                title = "No payments yet",
                subtitle = "Bills you mark as paid will show up here."
            )

            else -> HistoryContent(
                uiState = uiState,
                monthExpanded = monthExpanded,
                onMonthExpandedChange = { monthExpanded = it },
                onMonthSelected = viewModel::onMonthSelected
            )
        }
    }
}

@Composable
private fun HistoryContent(
    uiState: BillHistoryUiState,
    monthExpanded: Boolean,
    onMonthExpandedChange: (Boolean) -> Unit,
    onMonthSelected: (YearMonth?) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "month-filter") {
            MonthFilter(
                uiState = uiState,
                expanded = monthExpanded,
                onExpandedChange = onMonthExpandedChange,
                onMonthSelected = onMonthSelected
            )
        }

        item(key = "summaries") {
            SummaryRow(uiState)
        }

        uiState.visibleMonths.forEach { month ->
            item(key = "month-${month.yearMonth}") {
                MonthHeader(month)
            }
            items(month.payments, key = { "payment-${it.id}" }) { payment ->
                PaymentRow(payment)
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthFilter(
    uiState: BillHistoryUiState,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onMonthSelected: (YearMonth?) -> Unit
) {
    BillDropdownField(
        label = "Month",
        value = uiState.selectedMonthLabel,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        accent = LivelyIndigo
    ) {
        DropdownMenuItem(
            text = { Text(BillHistoryUiState.ALL_MONTHS_LABEL) },
            trailingIcon = {
                if (uiState.selectedMonth == null) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = LivelyIndigo
                    )
                }
            },
            onClick = {
                onMonthSelected(null)
                onExpandedChange(false)
            }
        )
        uiState.months.forEach { month ->
            DropdownMenuItem(
                text = { Text(month.shortLabel) },
                trailingIcon = {
                    if (uiState.selectedMonth == month.yearMonth) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = LivelyIndigo
                        )
                    }
                },
                onClick = {
                    onMonthSelected(month.yearMonth)
                    onExpandedChange(false)
                }
            )
        }
    }
}

@Composable
private fun SummaryRow(uiState: BillHistoryUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SummaryCard(
            label = "This month",
            total = uiState.thisMonthTotal,
            count = uiState.thisMonthCount,
            color = LivelyIndigo,
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            label = "Last month",
            total = uiState.lastMonthTotal,
            count = uiState.lastMonthCount,
            color = LivelyViolet,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryCard(
    label: String,
    total: Double,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    LivelySectionCard(
        accent = color,
        accentEnd = color,
        compact = true,
        modifier = modifier
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            total.toPesoAmount(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            formatHistoryCount(count),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MonthHeader(month: BillPaymentMonth) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            month.label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = LivelyIndigo
        )
        Spacer(Modifier.weight(1f))
        Text(
            "${month.count} • ${month.total.toPesoAmount()}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PaymentRow(payment: BillPayment) {
    LivelySectionCard(
        accent = LivelyBlue,
        accentEnd = LivelyBlue,
        compact = true,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LivelyIconBadge(
                icon = Icons.Default.Check,
                accent = LivelyBlue,
                accentEnd = LivelyBlue,
                size = 38.dp
            )
            Column(Modifier.weight(1f)) {
                Text(
                    payment.billName.ifBlank { "Bill payment" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Paid on ${payment.paidAtMillis.toDateString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Mode of Payment: ${payment.paymentMethod.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                payment.amount.toPesoAmount(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
