package com.smartexpense.feature.expenses.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpense.feature.expenses.intent.ExpenseIntent
import com.smartexpense.feature.expenses.viewmodel.ExpenseViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun ExpenseScreen(
    viewModel: ExpenseViewModel = koinViewModel(),
    onAddExpenseClick: () -> Unit,
    onExpenseClick : (Long) -> Unit
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
            title =  {
                Text(text = "Delete Expense")
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
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Expenses : ${uiState.expenses.size}"
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)

        ) {
            items(
                items = uiState.expenses,
                key = { expense -> expense.id }) { expense ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick =  {
                        onExpenseClick(expense.id)
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(text = "₱${expense.amount}")
                            Text(text = expense.description)
                        }
                    }

                    TextButton(
                        onClick = {
                            viewModel.onIntent(ExpenseIntent.RequestDeleteExpense(id = expense.id))
                        }

                    ) {
                        Text(text = "Delete")
                    }

                }
            }
        }
        Button(
            onClick = onAddExpenseClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Expense")
        }
    }
}