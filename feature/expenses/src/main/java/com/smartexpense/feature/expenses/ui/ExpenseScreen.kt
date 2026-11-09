package com.smartexpense.feature.expenses.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpense.core.common.toDateString
import com.smartexpense.core.common.toPesoAmount
import com.smartexpense.designsystems.ModernEmptyState
import com.smartexpense.designsystems.ModernHeader
import com.smartexpense.designsystems.ModernSectionHeader
import com.smartexpense.domain.model.Expense
import com.smartexpense.domain.model.ExpenseSyncStatus
import com.smartexpense.feature.expenses.intent.ExpenseIntent
import com.smartexpense.feature.expenses.viewmodel.ExpenseViewModel
import org.koin.androidx.compose.koinViewModel

private val PrimaryAccent = Color(0xFF2563EB)
private val PrimaryAccentEnd = Color(0xFF60A5FA)
private val SuccessColor = Color(0xFF3B82F6)
private val ErrorColor = Color(0xFF1E3A8A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    viewModel: ExpenseViewModel = koinViewModel(),
    onAddExpenseClick: () -> Unit,
    onExpenseClick: (Long) -> Unit,
    onAddExpenseCategoryClick: () -> Unit
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    if (screenState.showDeleteConfirmation) {

        AlertDialog(
            onDismissRequest = {
                viewModel.onIntent(
                    ExpenseIntent.CancelDeleteExpense
                )
            },
            title = {
                Text("Delete Expense")
            },
            text = {
                Text("Are you sure you want to delete this expense?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onIntent(ExpenseIntent.ConfirmDeleteExpense)
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.onIntent(ExpenseIntent.CancelDeleteExpense)
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    val expenses = uiState.expenses
    val syncedCount = expenses.count {
        it.syncStatus == ExpenseSyncStatus.SYNCED
    }
    val pendingCount = expenses.count {
        it.syncStatus == ExpenseSyncStatus.PENDING
    }
    val failedCount = expenses.count {
        it.syncStatus == ExpenseSyncStatus.FAILED
    }
    val syncCaption = when {
        failedCount > 0 -> "$failedCount need attention"
        pendingCount > 0 -> "$pendingCount pending sync"
        else -> "All synced"
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ModernHeader(
            title = "Expenses",
            subtitle = "Track and manage your spending",
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            actions = {
                HeaderActionPill(
                    label = "Manage categories",
                    icon = Icons.Default.Category,
                    onClick = onAddExpenseCategoryClick
                )
            }
        )

        if (expenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                ModernEmptyState(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    title = "No expenses yet",
                    subtitle = "Add your first expense to get started.",
                    action = {
                        GradientCta(
                            label = "Add your first expense",
                            icon = Icons.Default.Add,
                            onClick = onAddExpenseClick
                        )
                    }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 96.dp
                )
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ExpenseStatCard(
                            modifier = Modifier.weight(1f),
                            label = "Total spent",
                            value = uiState.totalAmount.toPesoAmount(),
                            caption = "Across all expenses",
                            icon = Icons.Default.Payments,
                            accent = PrimaryAccent,
                            accentEnd = PrimaryAccentEnd
                        )
                        ExpenseStatCard(
                            modifier = Modifier.weight(1f),
                            label = "Expenses",
                            value = expenses.size.toString(),
                            caption = syncCaption,
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            accent = SuccessColor,
                            accentEnd = Color(0xFF93C5FD)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    ModernSectionHeader(title = "All expenses")
                }

                items(
                    items = expenses,
                    key = { expense -> expense.id }
                ) { expense ->
                    ExpenseListCard(
                        expense = expense,
                        onClick = {
                            onExpenseClick(expense.id)
                        },
                        onRetry = {
                            viewModel.onIntent(
                                ExpenseIntent.RetrySync(expense.id)
                            )
                        },
                        onDelete = {
                            viewModel.onIntent(
                                ExpenseIntent.RequestDeleteExpense(
                                    id = expense.id
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderActionPill(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val pillShape = RoundedCornerShape(percent = 50)

    Surface(
        onClick = onClick,
        shape = pillShape,
        color = Color.White.copy(alpha = 0.18f),
        contentColor = Color.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun GradientCta(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(percent = 50)

    Row(
        modifier = Modifier
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(PrimaryAccent, PrimaryAccentEnd)
                )
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color.White
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

@Composable
private fun ExpenseStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    caption: String,
    icon: ImageVector,
    accent: Color,
    accentEnd: Color
) {
    val colorScheme = MaterialTheme.colorScheme
    val chipShape = RoundedCornerShape(14.dp)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.16f),
                            colorScheme.surface,
                            accent.copy(alpha = 0.03f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(chipShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    accent.copy(alpha = 0.75f),
                                    accentEnd
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.25f),
                            shape = chipShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseListCard(
    expense: Expense,
    onClick: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val accent = when (expense.syncStatus) {
        ExpenseSyncStatus.SYNCED -> SuccessColor
        ExpenseSyncStatus.FAILED -> ErrorColor
        ExpenseSyncStatus.PENDING -> PrimaryAccent
    }
    val chipShape = RoundedCornerShape(15.dp)
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.10f),
                            colorScheme.surface,
                            accent.copy(alpha = 0.02f)
                        )
                    )
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(chipShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.75f),
                                accent
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.25f),
                        shape = chipShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    modifier = Modifier.size(23.dp),
                    tint = Color.White
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.description.ifBlank { "Expense" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = expense.date.toDateString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(accent)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = expense.syncStatus.label(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = accent,
                        maxLines = 1
                    )
                }
            }

            Text(
                text = expense.amount.toPesoAmount(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                maxLines = 1
            )

            Box {
                IconButton(
                    onClick = { menuExpanded = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Expense actions",
                        modifier = Modifier.size(18.dp),
                        tint = colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    if (expense.syncStatus == ExpenseSyncStatus.FAILED) {
                        DropdownMenuItem(
                            text = { Text("Retry sync") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = PrimaryAccent
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onRetry()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Delete",
                                color = ErrorColor
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = ErrorColor
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
