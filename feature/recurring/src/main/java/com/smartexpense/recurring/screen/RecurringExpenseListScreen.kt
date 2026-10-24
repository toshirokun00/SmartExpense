package com.smartexpense.recurring.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpense.core.common.toDateString
import com.smartexpense.core.common.toPesoAmount
import com.smartexpense.domain.model.RecurringScheduleStatus
import com.smartexpense.recurring.effect.RecurringExpenseEffect
import com.smartexpense.recurring.intent.RecurringExpenseIntent
import com.smartexpense.recurring.state.RecurringExpenseSummaryState
import com.smartexpense.recurring.viewmodel.RecurringExpenseListViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringExpenseListScreen(
    viewModel: RecurringExpenseListViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onAddSchedule: () -> Unit,
    onEditSchedule: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            val message = when (effect) {
                is RecurringExpenseEffect.ShowError -> effect.message
                RecurringExpenseEffect.SchedulePaused -> "Schedule paused"
                RecurringExpenseEffect.ScheduleResumed -> "Schedule resumed"
                RecurringExpenseEffect.ScheduleDeleted -> "Schedule deleted"
            }

            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = {
                    Text("Recurring Expenses")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddSchedule
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add recurring expense"
                )
            }
        }
    ) { paddingValues ->
        val errorMessage = uiState.errorMessage

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            uiState.schedules.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No recurring expenses yet.")
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.schedules,
                        key = { summary -> summary.schedule.id }
                    ) { summary ->
                        RecurringScheduleCard(
                            summary = summary,
                            onEdit = {
                                onEditSchedule(summary.schedule.id)
                            },
                            onToggleActive = {
                                if (summary.schedule.isActive) {
                                    viewModel.onIntent(
                                        RecurringExpenseIntent.Pause(
                                            summary.schedule.id
                                        )
                                    )
                                } else {
                                    viewModel.onIntent(
                                        RecurringExpenseIntent.Resume(
                                            summary.schedule.id
                                        )
                                    )
                                }
                            },
                            onDelete = {
                                viewModel.onIntent(
                                    RecurringExpenseIntent.RequestDelete(
                                        summary.schedule.id
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    if (screenState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = {
                viewModel.onIntent(RecurringExpenseIntent.CancelDelete)
            },
            title = {
                Text("Delete recurring expense?")
            },
            text = {
                Text(
                    "Generated expenses stay in your history. " +
                        "No new occurrences will be created."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onIntent(RecurringExpenseIntent.ConfirmDelete)
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.onIntent(RecurringExpenseIntent.CancelDelete)
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RecurringScheduleCard(
    summary: RecurringExpenseSummaryState,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = summary.schedule.description,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = summary.schedule.amount.toPesoAmount(),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${summary.categoryName} • ${summary.frequencyLabel}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = summary.statusLabel(),
                style = MaterialTheme.typography.bodySmall,
                color = when (summary.status) {
                    RecurringScheduleStatus.ACTIVE ->
                        MaterialTheme.colorScheme.primary

                    RecurringScheduleStatus.PAUSED ->
                        MaterialTheme.colorScheme.tertiary

                    RecurringScheduleStatus.ENDED ->
                        MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            if (summary.status != RecurringScheduleStatus.ENDED) {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Next: ${summary.schedule.nextOccurrence.toDateString()}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit recurring expense"
                    )
                }

                if (summary.status != RecurringScheduleStatus.ENDED) {
                    IconButton(onClick = onToggleActive) {
                        Icon(
                            imageVector = if (summary.schedule.isActive) {
                                Icons.Default.Pause
                            } else {
                                Icons.Default.PlayArrow
                            },
                            contentDescription = if (summary.schedule.isActive) {
                                "Pause recurring expense"
                            } else {
                                "Resume recurring expense"
                            }
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete recurring expense"
                    )
                }
            }
        }
    }
}

private fun RecurringExpenseSummaryState.statusLabel(): String {
    return when (status) {
        RecurringScheduleStatus.ACTIVE -> "Active"
        RecurringScheduleStatus.PAUSED -> "Paused"
        RecurringScheduleStatus.ENDED -> "Ended"
    }
}
