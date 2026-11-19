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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.smartexpense.domain.model.PaycheckEnvelope
import com.smartexpense.paycheck.state.MoneyTrackerUiState
import com.smartexpense.paycheck.viewmodel.MoneyTrackerViewModel
import org.koin.androidx.compose.koinViewModel
import java.time.format.DateTimeFormatter

@Composable
fun MoneyTrackerScreen(
    viewModel: MoneyTrackerViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onPaycheckClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        ModernHeader(
            title = "Money Tracker",
            subtitle = "Income minus bills and spending",
            icon = Icons.Default.Savings,
            onBackClick = onNavigateBack
        )

        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            uiState.envelopes.isEmpty() -> ModernEmptyState(
                icon = Icons.Default.Savings,
                title = "Nothing to track yet",
                subtitle = "Add a paycheck and assign bills to see what money is left."
            )

            else -> TrackerList(
                uiState = uiState,
                onPaycheckClick = onPaycheckClick
            )
        }
    }
}

@Composable
private fun TrackerList(
    uiState: MoneyTrackerUiState,
    onPaycheckClick: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            TrackerSummaryCard(uiState = uiState)
        }
        items(uiState.envelopes, key = { it.paycheck.id }) { envelope ->
            EnvelopeCard(
                envelope = envelope,
                onClick = { onPaycheckClick(envelope.paycheck.id) }
            )
        }
        item {
            Text(
                text = "Expenses are counted from each payday until the next " +
                    "payday.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun TrackerSummaryCard(uiState: MoneyTrackerUiState) {
    val accent = if (uiState.isShort) LivelyRose else LivelyIndigo

    LivelySectionCard(
        accent = accent,
        accentEnd = LivelyViolet,
        title = "Plan overview",
        icon = Icons.Default.AccountBalanceWallet
    ) {
        Text(
            text = if (uiState.isShort) "Short by" else "Money left",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = uiState.totalRemaining.toPesoAmount(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Medium,
            color = accent
        )

        Spacer(Modifier.height(10.dp))
        LivelyProgressBar(
            progress = uiState.usedFraction,
            color = accent,
            trackColor = accent.copy(alpha = 0.14f),
            barHeight = 10.dp,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryStat(
                label = "Income",
                value = uiState.totalIncome,
                color = LivelyBlue,
                modifier = Modifier.weight(1f)
            )
            SummaryStat(
                label = "Bills",
                value = uiState.totalBills,
                color = LivelyViolet,
                modifier = Modifier.weight(1f)
            )
            SummaryStat(
                label = "Expenses",
                value = uiState.totalExpenses,
                color = LivelyRose,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryStat(
    label: String,
    value: Double,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value.toPesoAmount(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EnvelopeCard(
    envelope: PaycheckEnvelope,
    onClick: () -> Unit
) {
    val accent = if (envelope.isShort) LivelyRose else LivelyIndigo
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd") }

    LivelySectionCard(
        accent = accent,
        accentEnd = LivelyViolet,
        compact = true,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
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
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LivelyIconBadge(
                    icon = Icons.Default.Payments,
                    accent = accent,
                    accentEnd = LivelyViolet,
                    size = 40.dp,
                    shape = RoundedCornerShape(12.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = envelope.paycheck.displayLabel(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.height(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = cycleLabel(envelope, dateFormatter),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = envelope.income.toPesoAmount(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Text(
                        text = "income",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DeductionStat(
                    label = "Bills",
                    value = envelope.billsTotal,
                    color = LivelyViolet,
                    modifier = Modifier.weight(1f)
                )
                DeductionStat(
                    label = "Expenses",
                    value = envelope.expensesTotal,
                    color = LivelyRose,
                    modifier = Modifier.weight(1f)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Left",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = envelope.remaining.toPesoAmount(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            LivelyProgressBar(
                progress = envelope.usedFraction,
                color = accent,
                trackColor = accent.copy(alpha = 0.14f),
                barHeight = 8.dp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${envelope.assignedBills.size} " +
                        if (envelope.assignedBills.size == 1) "bill" else "bills",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${envelope.expenses.size} " +
                        if (envelope.expenses.size == 1) "expense" else "expenses",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DeductionStat(
    label: String,
    value: Double,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.height(12.dp),
                tint = color
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = "−${value.toPesoAmount()}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun cycleLabel(
    envelope: PaycheckEnvelope,
    formatter: DateTimeFormatter
): String {
    val start = envelope.cycleStartDate()
    val end = envelope.cycleLastDate()
    val range = "${start.format(formatter)} – ${end.format(formatter)}"
    return if (start.year == end.year) {
        range
    } else {
        "$start – $end"
    }
}
