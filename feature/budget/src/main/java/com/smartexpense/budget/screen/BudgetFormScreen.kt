package com.smartexpense.budget.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpense.core.common.toDateString
import com.smartexpense.designsystems.ModernHeader
import com.smartexpense.designsystems.MoneyAmountField
import com.smartexpense.budget.effect.BudgetEffect
import com.smartexpense.budget.intent.BudgetIntent
import com.smartexpense.budget.viewmodel.BudgetViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetFormScreen(
    viewModel: BudgetViewModel = koinViewModel(),
    budgetId: Long? = null,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(budgetId) {
        if (budgetId != null) {
            viewModel.onIntent(BudgetIntent.LoadBudget(budgetId))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                BudgetEffect.BudgetAdded -> {
                    onSaved()
                }
                BudgetEffect.BudgetUpdated -> {
                    onSaved()
                }
                BudgetEffect.BudgetDeleted -> {
                    onSaved()
                }
                is BudgetEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        // The header draws behind the status bar and handles its own inset.
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding()
        ) {
            ModernHeader(
                title = if (budgetId == null) "Add Budget" else "Edit Budget",
                subtitle = "Set limits and track spending by category",
                icon = Icons.Default.Wallet,
                onBackClick = onNavigateBack
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Category,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }

                                Text(
                                    text = if (budgetId == null) "New Budget" else "Edit Budget",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            ExposedDropdownMenuBox(
                                expanded = showCategoryDropdown,
                                onExpandedChange = {
                                    showCategoryDropdown = !showCategoryDropdown
                                }
                            ) {
                                OutlinedTextField(
                                    value = uiState.categories
                                        .firstOrNull {
                                            it.id == uiState.form.categoryId
                                        }
                                        ?.name
                                        ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = {
                                        Text("Category")
                                    },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                            expanded = showCategoryDropdown
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )

                                ExposedDropdownMenu(
                                    expanded = showCategoryDropdown,
                                    onDismissRequest = {
                                        showCategoryDropdown = false
                                    }
                                ) {
                                    uiState.categories.forEach { category ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(category.name)
                                            },
                                            onClick = {
                                                viewModel.onIntent(
                                                    BudgetIntent.CategoryChanged(
                                                        category.id
                                                    )
                                                )
                                                showCategoryDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            MoneyAmountField(
                                value = uiState.form.amount,
                                onValueChange = {
                                    viewModel.onIntent(BudgetIntent.AmountChanged(it))
                                },
                                label = "Budget Amount",
                                prefix = "₱",
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = uiState.form.startDate
                                        ?.toDateString()
                                        ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = {
                                        Text("Start Date")
                                    },
                                    modifier = Modifier.weight(1f)
                                        .padding(end = 6.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    trailingIcon = {
                                        IconButton(onClick = { showStartDatePicker = true }) {
                                            Icon(
                                                imageVector = Icons.Default.DateRange,
                                                contentDescription = "Pick start date"
                                            )
                                        }
                                    }
                                )

                                OutlinedTextField(
                                    value = uiState.form.endDate
                                        ?.toDateString()
                                        ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = {
                                        Text("End Date")
                                    },
                                    modifier = Modifier.weight(1f)
                                        .padding(start = 6.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    trailingIcon = {
                                        IconButton(onClick = { showEndDatePicker = true }) {
                                            Icon(
                                                imageVector = Icons.Default.DateRange,
                                                contentDescription = "Pick end date"
                                            )
                                        }
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.secondary,
                                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                                            )
                                        )
                                    )
                                    .clickable(
                                        enabled = !uiState.form.isSaving,
                                        onClick = {
                                            if (budgetId == null) {
                                                viewModel.onIntent(BudgetIntent.AddBudget)
                                            } else {
                                                viewModel.onIntent(BudgetIntent.UpdateBudget)
                                            }
                                        }
                                    )
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (uiState.form.isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onSecondary
                                    )
                                } else {
                                    Text(
                                        text = if (budgetId == null) "Add Budget" else "Update Budget",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.form.startDate ?: System.currentTimeMillis()
        )

        DatePickerDialog(
            onDismissRequest = {
                showStartDatePicker = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { date ->
                            viewModel.onIntent(BudgetIntent.StartDateChanged(date))
                        }
                        showStartDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.form.endDate ?: uiState.form.startDate ?: System.currentTimeMillis()
        )

        DatePickerDialog(
            onDismissRequest = {
                showEndDatePicker = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { date ->
                            viewModel.onIntent(BudgetIntent.EndDateChanged(date))
                        }
                        showEndDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
