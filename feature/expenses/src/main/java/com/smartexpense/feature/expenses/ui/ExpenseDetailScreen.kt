package com.smartexpense.feature.expenses.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpense.domain.model.ExpenseSyncStatus
import com.smartexpense.feature.expenses.effect.ExpenseEffect
import com.smartexpense.feature.expenses.intent.ExpenseIntent
import com.smartexpense.feature.expenses.viewmodel.ExpenseViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(
    expenseId : Long,
    viewModel: ExpenseViewModel = koinViewModel(),
    onBackClick : () -> Unit,
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackBarHostState = remember { SnackbarHostState() }

    LaunchedEffect(expenseId) {
        viewModel.onIntent(ExpenseIntent.LoadExpense(expenseId))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when(effect) {
                ExpenseEffect.ExpenseAdded -> Unit
                ExpenseEffect.ExpenseUpdated -> {
                    onBackClick()
                }
                ExpenseEffect.NavigateToAddExpense -> Unit
                is ExpenseEffect.ShowError -> {
                    snackBarHostState.showSnackbar(effect.message)
                }
                ExpenseEffect.SyncRetryRequested -> {
                    snackBarHostState.showSnackbar("Sync retry requested")
                }
                ExpenseEffect.ExpenseDeleted -> Unit
            }

        }
    }

    val currentExpense = uiState.expenses.find { it.id == expenseId }
    val syncStatus = currentExpense?.syncStatus ?: uiState.form.syncStatus

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Edit Expense")
                },
                navigationIcon =  {
                    IconButton(
                        onBackClick
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState)
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            MoneyAmountField(
                value = uiState.form.amount,
                onValueChange = {
                    viewModel.onIntent(ExpenseIntent.AmountChanged(it))
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.form.description,
                onValueChange = {
                    viewModel.onIntent(ExpenseIntent.DescriptionChanged(it))
                },
                label = {
                    Text("Description")
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.form.merchant,
                onValueChange = {
                    viewModel.onIntent(ExpenseIntent.MerchantChanged(it))
                },
                label = {
                    Text("Merchant / Payee")
                },
                modifier = Modifier.fillMaxWidth()
            )

            ExpenseDateField(
                dateMillis = uiState.form.date,
                onDateSelected = {
                    viewModel.onIntent(ExpenseIntent.DateChanged(it))
                }
            )

            SyncStatusRow(
                status = syncStatus,
                onRetry = {
                    viewModel.onIntent(ExpenseIntent.RetrySync(expenseId))
                }
            )

            Button(
                onClick = {
                  viewModel.onIntent(ExpenseIntent.UpdateExpense(id = expenseId))
                },
                enabled = !uiState.form.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.form.isSaving) "Saving..." else "Save Changes")
            }
        }

    }
}

@Composable
private fun SyncStatusRow(
    status: ExpenseSyncStatus,
    onRetry: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Sync status: ${status.label()}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (status == ExpenseSyncStatus.FAILED) {
            TextButton(onClick = onRetry) {
                Text("Retry sync")
            }
        }
    }
}

internal fun ExpenseSyncStatus.label(): String = when (this) {
    ExpenseSyncStatus.PENDING -> "Pending"
    ExpenseSyncStatus.SYNCED -> "Synced"
    ExpenseSyncStatus.FAILED -> "Failed"
}
