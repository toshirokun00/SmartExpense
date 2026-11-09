package com.smartexpense.recurring.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpense.core.common.toDateString
import com.smartexpense.core.common.toPesoAmount
import com.smartexpense.designsystems.*
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ModernHeader(
                title = "Recurring Expenses",
                subtitle = "Manage your scheduled expenses",
                icon = Icons.Default.Loop,
                onBackClick = onNavigateBack,
                actions = {
                    Button(
                        onClick = onAddSchedule,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.25f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Schedule")
                    }
                }
            )

            AnimatedVisibility(
                visible = uiState.isLoading,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                ModernEmptyState(
                    icon = Icons.Default.Loop,
                    title = "Something went wrong",
                    subtitle = uiState.errorMessage
                )
            }

            if (!uiState.isLoading && uiState.errorMessage == null) {
                if (uiState.schedules.isEmpty()) {
                    ModernEmptyState(
                        icon = Icons.Default.Loop,
                        title = "No recurring expenses yet",
                        subtitle = "Tap Add Schedule to create your first scheduled expense.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(paddingValues)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(16.dp)
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
    val statusColor = when (summary.status) {
        RecurringScheduleStatus.ACTIVE -> ModernSuccessColor
        RecurringScheduleStatus.PAUSED -> ModernWarningColor
        RecurringScheduleStatus.ENDED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val accentEnd = when (summary.status) {
        RecurringScheduleStatus.ACTIVE -> LivelyTeal
        RecurringScheduleStatus.PAUSED -> LivelyOrange
        RecurringScheduleStatus.ENDED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            statusColor.copy(alpha = 0.10f),
                            MaterialTheme.colorScheme.surface,
                            statusColor.copy(alpha = 0.02f)
                        )
                    )
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LivelyIconBadge(
                icon = Icons.Default.Loop,
                accent = statusColor,
                accentEnd = accentEnd,
                size = 46.dp,
                shape = RoundedCornerShape(15.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.schedule.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${summary.categoryName} • ${summary.frequencyLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = summary.statusLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (summary.status != RecurringScheduleStatus.ENDED) {
                        Text(
                            text = "Next: ${summary.schedule.nextOccurrence.toDateString()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
            ) {
                Text(
                    text = summary.schedule.amount.toPesoAmount(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit recurring expense",
                            tint = LivelyIndigo
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
                                },
                                tint = LivelyOrange
                            )
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete recurring expense",
                            tint = LivelyRose
                        )
                    }
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
