package com.smartexpense.bills.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpense.bills.intent.BillsIntent
import com.smartexpense.bills.util.formatAmountForEditing
import com.smartexpense.bills.viewmodel.BillsViewModel
import com.smartexpense.core.common.toDateString
import com.smartexpense.core.common.toPesoAmount
import com.smartexpense.core.common.util.MoneyInputUtil
import com.smartexpense.designsystems.LivelyBlue
import com.smartexpense.designsystems.LivelyIconBadge
import com.smartexpense.designsystems.LivelyIndigo
import com.smartexpense.designsystems.LivelyRose
import com.smartexpense.designsystems.LivelySectionCard
import com.smartexpense.designsystems.LivelyToggleRow
import com.smartexpense.designsystems.LivelyViolet
import com.smartexpense.designsystems.ModernEmptyState
import com.smartexpense.designsystems.ModernHeader
import com.smartexpense.designsystems.ModernHeaderActionPill
import com.smartexpense.designsystems.MoneyAmountField
import com.smartexpense.domain.model.Bill
import com.smartexpense.domain.model.BillAmountType
import com.smartexpense.domain.model.BillStatus
import com.smartexpense.domain.model.PaymentMethod
import com.smartexpense.feature.expenses.ui.ExpenseDateField
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillsScreen(
    viewModel: BillsViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onAddBill: () -> Unit,
    onHistory: () -> Unit,
    onEditBill: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val now = System.currentTimeMillis()

    Column(Modifier.fillMaxSize()) {
        ModernHeader(
            title = "Bills",
            subtitle = "Track and pay your bills",
            icon = Icons.Default.Receipt,
            onBackClick = onNavigateBack,
            actions = {
                ModernHeaderActionPill(
                    label = "History",
                    icon = Icons.Default.History,
                    onClick = onHistory
                )
                ModernHeaderActionPill(
                    label = "Add Bill",
                    icon = Icons.Default.Add,
                    onClick = onAddBill
                )
            }
        )

        if (uiState.bills.isEmpty() && !uiState.isLoading) {
            ModernEmptyState(
                icon = Icons.Default.Receipt,
                title = "No bills yet",
                subtitle = "Tap Add Bill to add your first bill."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BILL_GROUPS.forEach { group ->
                    val section = uiState.bills
                        .filter { it.status(now) in group.statuses }
                        .sortedBy { it.dueDateMillis }

                    if (section.isNotEmpty()) {
                        item(key = "header-${group.title}") {
                            BillGroupHeader(
                                title = group.title,
                                count = section.size,
                                total = section.sumOf { it.amount },
                                color = group.accent
                            )
                        }
                        items(section, key = { "bill-${it.id}" }) { bill ->
                            BillCard(
                                bill = bill,
                                status = bill.status(now),
                                needsAmount = bill.needsAmount(now),
                                categoryName = uiState.categories
                                    .firstOrNull { it.id == bill.categoryId }
                                    ?.name,
                                onClick = { onEditBill(bill.id) },
                                onMarkPaid = {
                                    viewModel.onIntent(BillsIntent.ShowPaySheet(bill))
                                }
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }

    val payingBill = uiState.payingBill
    if (payingBill != null) {
        BillPaySheet(
            bill = payingBill,
            onDismiss = { viewModel.onIntent(BillsIntent.HidePaySheet) },
            onPay = { amount, paidDate, method, createExpense ->
                viewModel.onIntent(
                    BillsIntent.Pay(
                        id = payingBill.id,
                        amount = amount,
                        paidAtMillis = paidDate,
                        paymentMethod = method,
                        createExpense = createExpense
                    )
                )
            }
        )
    }
}

// ---------------------------------------------------------------------------
// List
// ---------------------------------------------------------------------------

private data class BillGroup(
    val title: String,
    val statuses: Set<BillStatus>,
    val accent: Color
)

private val BILL_GROUPS = listOf(
    BillGroup("Overdue", setOf(BillStatus.OVERDUE), LivelyRose),
    BillGroup(
        "Due this week",
        setOf(BillStatus.DUE_TODAY, BillStatus.DUE_SOON),
        LivelyIndigo
    ),
    BillGroup("Upcoming", setOf(BillStatus.UPCOMING), LivelyViolet),
    BillGroup("Paid", setOf(BillStatus.PAID, BillStatus.SKIPPED), LivelyBlue),
    BillGroup("Paused", setOf(BillStatus.PAUSED), Color(0xFF64748B))
)

@Composable
private fun BillGroupHeader(
    title: String,
    count: Int,
    total: Double,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(Modifier.weight(1f))
        Text(
            "$count • ${total.toPesoAmount()}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BillCard(
    bill: Bill,
    status: BillStatus,
    needsAmount: Boolean,
    categoryName: String?,
    onClick: () -> Unit,
    onMarkPaid: () -> Unit
) {
    val accent = when (status) {
        BillStatus.OVERDUE -> LivelyRose
        BillStatus.DUE_TODAY,
        BillStatus.DUE_SOON -> LivelyIndigo
        BillStatus.PAID,
        BillStatus.SKIPPED -> LivelyBlue
        BillStatus.PAUSED -> Color(0xFF64748B)
        BillStatus.UPCOMING -> LivelyViolet
    }

    LivelySectionCard(
        accent = accent,
        accentEnd = accent,
        compact = true,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LivelyIconBadge(
                icon = when {
                    bill.skipped -> Icons.Default.SkipNext
                    bill.isRecurring -> Icons.Default.Repeat
                    else -> Icons.Default.Receipt
                },
                accent = accent,
                accentEnd = accent,
                size = 38.dp,
                shape = RoundedCornerShape(12.dp)
            )
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        bill.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(6.dp))
                    StatusChip(status.label(), accent)
                }
                Text(
                    "${if (bill.isRecurring) "Next" else "Due"} ${bill.dueDateMillis.toDateString()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val meta = buildList {
                    add(bill.repeatLabel())
                    categoryName?.let { add(it) }
                    if (bill.autopay) add("Auto-pay")
                }
                Text(
                    meta.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(Modifier.height(3.dp))
                StatusChip(
                    label = if (bill.amountType == BillAmountType.VARIABLE) {
                        "Variable"
                    } else {
                        "Fixed"
                    },
                    color = if (needsAmount) LivelyRose else LivelyIndigo
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (bill.amountEstimated) {
                        "Est. ${bill.amount.toPesoAmount()}"
                    } else {
                        bill.amount.toPesoAmount()
                    },
                    fontWeight = FontWeight.Bold
                )
                if (status == BillStatus.PAID) {
                    Text(
                        bill.paidAtMillis?.toDateString() ?: "Paid",
                        style = MaterialTheme.typography.labelSmall,
                        color = LivelyBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                } else if (status != BillStatus.SKIPPED && status != BillStatus.PAUSED) {
                    TextButton(
                        onClick = onMarkPaid,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("Mark paid", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, color: Color) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = color,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

// ---------------------------------------------------------------------------
// Mark-as-paid sheet
// ---------------------------------------------------------------------------

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillPaySheet(
    bill: Bill,
    onDismiss: () -> Unit,
    onPay: (
        amount: Double,
        paidDate: Long,
        paymentMethod: PaymentMethod,
        createExpense: Boolean
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amount by remember(bill.id) { mutableStateOf(formatAmountForEditing(bill.amount)) }
    var paidDate by remember(bill.id) { mutableStateOf(System.currentTimeMillis()) }
    var method by remember(bill.id) { mutableStateOf(bill.paymentMethod) }
    var methodExpanded by remember(bill.id) { mutableStateOf(false) }
    var createExpense by remember(bill.id) { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LivelyIconBadge(
                    icon = Icons.Default.Payments,
                    accent = LivelyIndigo,
                    accentEnd = LivelyViolet,
                    size = 42.dp,
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Mark as paid",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        bill.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (bill.amountType == BillAmountType.VARIABLE && bill.amountEstimated) {
                LivelySectionCard(accent = LivelyBlue, accentEnd = LivelyViolet) {
                    Text(
                        "Estimated ${bill.amount.toPesoAmount()} based on recent payments. " +
                            "Enter the actual amount from your bill.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            MoneyAmountField(
                value = amount,
                onValueChange = { amount = it },
                label = if (bill.amountType == BillAmountType.VARIABLE) {
                    "Actual amount paid"
                } else {
                    "Amount paid"
                },
                prefix = "₱"
            )
            ExpenseDateField(
                dateMillis = paidDate,
                onDateSelected = { paidDate = it },
                label = "Paid on",
                accentColor = LivelyIndigo
            )

            BillDropdownField(
                label = "Payment method",
                value = method.displayName,
                expanded = methodExpanded,
                onExpandedChange = { methodExpanded = it },
                accent = LivelyBlue
            ) {
                PaymentMethod.entries.toList().forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.displayName) },
                        trailingIcon = {
                            if (method == option) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = LivelyBlue
                                )
                            }
                        },
                        onClick = {
                            method = option
                            methodExpanded = false
                        }
                    )
                }
            }

            LivelyToggleRow(
                title = "Create expense",
                subtitle = "Add this payment to your spending and budgets",
                icon = Icons.Default.Receipt,
                accent = LivelyIndigo,
                checked = createExpense,
                onCheckedChange = { createExpense = it }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(percent = 50)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val parsed = MoneyInputUtil.cleanAmount(amount).toDoubleOrNull()
                        if (parsed != null && parsed > 0) {
                            onPay(parsed, paidDate, method, createExpense)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("bill_pay_confirm"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LivelyIndigo,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(percent = 50)
                ) {
                    Text("Mark paid", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

