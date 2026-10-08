package com.smartexpense.dashoard.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpense.core.common.toPesoAmount
import com.smartexpense.dashoard.viewmodel.DashboardViewModel
import com.smartexpense.dashoard.intent.DashboardIntent
import org.koin.androidx.compose.koinViewModel

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Dashboard",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Total Expenses",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = uiState.totalAmount.toPesoAmount(),
                    style = MaterialTheme.typography.headlineLarge
                )
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = "Recent Expenses",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        if (uiState.recentExpenses.isEmpty() && !uiState.isLoading) {
            Text(
                text = "No expenses yet."
            )
        } else {
            LazyColumn {
                items(
                    items = uiState.recentExpenses,
                    key = { it.id }
                ) { expense ->

                    Text(
                        text = "${expense.description} - ${
                            expense.amount.toPesoAmount()
                        }"
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Button(
            onClick = onNavigateToExpenses,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Expenses")
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Button(
            onClick = onNavigateToCategories,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Categories")
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Button(
            onClick = onNavigateToBudgets,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Budgets")
        }
    }


}