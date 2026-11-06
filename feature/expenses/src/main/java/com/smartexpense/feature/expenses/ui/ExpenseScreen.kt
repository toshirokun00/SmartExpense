package com.smartexpense.feature.expenses.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpense.core.common.toDateString
import com.smartexpense.core.common.toPesoAmount
import com.smartexpense.designsystems.ModernHeader
import com.smartexpense.designsystems.ModernListItemCard
import com.smartexpense.designsystems.ModernSectionHeader
import com.smartexpense.domain.model.ExpenseSyncStatus
import com.smartexpense.feature.expenses.intent.ExpenseIntent
import com.smartexpense.feature.expenses.viewmodel.ExpenseViewModel
import org.koin.androidx.compose.koinViewModel

private val SuccessColor = androidx.compose.ui.graphics.Color(0xFF10B981)

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

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ModernHeader(
            title = "Expenses",
            subtitle = "Track and manage your spending",
            icon = Icons.AutoMirrored.Filled.ReceiptLong
        )

        // Modern summary stats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatChip(
                label = "Expenses",
                value = uiState.expenses.size.toString(),
                modifier = Modifier.weight(1f)
            )
            StatChip(
                label = "Total",
                value = uiState.totalAmount.toPesoAmount(),
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }

        if (uiState.expenses.isEmpty()) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                com.smartexpense.designsystems.ModernEmptyState(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    title = "No expenses yet",
                    subtitle = "Add your first expense to get started."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(
                    items = uiState.expenses,
                    key = { expense -> expense.id }
                ) { expense ->
                    ModernListItemCard(
                        avatarIcon = Icons.AutoMirrored.Filled.ReceiptLong,
                        avatarColor = if (expense.syncStatus == ExpenseSyncStatus.SYNCED) {
                            SuccessColor
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        title = expense.description.ifBlank { "Expense" },
                        subtitle = expense.amount.toPesoAmount(),
                        trailing = {
                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = expense.date.toDateString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (expense.syncStatus == ExpenseSyncStatus.FAILED) {
                                    TextButton(
                                        onClick = {
                                            viewModel.onIntent(
                                                ExpenseIntent.RetrySync(expense.id)
                                            )
                                        }
                                    ) {
                                        Text("Retry")
                                    }
                                }
                                TextButton(
                                    onClick = {
                                        viewModel.onIntent(ExpenseIntent.RequestDeleteExpense(id = expense.id))
                                    }
                                ) {
                                    Text(
                                        text = "Delete",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        },
                        onClick = {
                            onExpenseClick(expense.id)
                        },
                        borderless = true
                    )
                }
            }
        }

        // Modern action bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onAddExpenseClick,
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Expense")
                }
            }

            OutlinedButton(
                onClick = onAddExpenseCategoryClick,
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Categories")
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    accentColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFA855F7)
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}
