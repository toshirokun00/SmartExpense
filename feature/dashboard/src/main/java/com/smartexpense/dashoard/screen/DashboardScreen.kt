package com.smartexpense.dashoard.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpense.core.common.toPesoAmount
import com.smartexpense.dashoard.viewmodel.DashboardViewModel
import com.smartexpense.dashoard.intent.DashboardIntent
import com.smartexpense.domain.model.Expense
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = koinViewModel(),
    onNavigateToExpenses: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToBudgets: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onIntent(
            DashboardIntent.LoadDashboard
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("SmartExpense")
                }
            )
        }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Overview",
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            item {
                SummaryCard(
                    title = "Total Expenses",
                    value = uiState.totalExpense.toPesoAmount()
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SmallSummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "Expenses",
                        value = uiState.expenseCount.toString()
                    )

                    SmallSummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "Total Budget",
                        value = uiState.totalBudget.toPesoAmount()
                    )
                }
            }
            item {
                BudgetOverviewCard(
                    totalBudget = uiState.totalBudget,
                    spent = uiState.totalBudgetSpent,
                    remaining = uiState.totalBudgetRemaining,
                    progress = uiState.budgetProgress,
                    isOverBudget = uiState.isOverBudget
                )
            }

            item {
                Text(
                    text = "Recent Expenses",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            if (uiState.recentExpenses.isEmpty()) {
                item {
                    Text(
                        text = "No expenses yet.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(
                    items = uiState.recentExpenses,
                    key = { it.id }
                ) { expense ->

                    RecentExpenseItem(
                        expense = expense
                    )
                }
            }

            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToExpenses
                    ) {
                        Text("Expenses")
                    }

                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToBudgets
                    ) {
                        Text("Budgets")
                    }
                }
            }
            item {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToCategories
                ) {
                    Text("Manage Categories")
                }
            }
        }
    }


}

@Composable
private fun SummaryCard(
    title: String,
    value: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}


@Composable
private fun SmallSummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
private fun BudgetOverviewCard(
    totalBudget: Double,
    spent: Double,
    remaining: Double,
    progress: Float,
    isOverBudget: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Budget Overview",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Budget: ${totalBudget.toPesoAmount()}"
            )

            Text(
                text = "Spent: ${spent.toPesoAmount()}"
            )

            if (isOverBudget) {
                Text(
                    text = "Over budget by ${(-remaining).toPesoAmount()}"
                )
            } else {
                Text(
                    text = "Remaining: ${remaining.toPesoAmount()}"
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "${(progress * 100).toInt()}% used"
            )
        }
    }
}

@Composable
private fun RecentExpenseItem(
    expense: Expense
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = expense.description.ifBlank {
                        "Expense"
                    },
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = formatDate(expense.date),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = expense.amount.toPesoAmount(),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat(
        "MMM dd, yyyy",
        Locale.getDefault()
    )

    return formatter.format(Date(timestamp))
}