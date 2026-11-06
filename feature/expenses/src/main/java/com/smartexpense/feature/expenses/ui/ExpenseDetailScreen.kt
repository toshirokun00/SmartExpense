package com.smartexpense.feature.expenses.ui

import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpense.domain.model.ExpenseSyncStatus
import com.smartexpense.domain.model.ExpenseType
import com.smartexpense.domain.model.PaymentMethod
import com.smartexpense.feature.expenses.effect.ExpenseEffect
import com.smartexpense.feature.expenses.intent.ExpenseIntent
import com.smartexpense.feature.expenses.viewmodel.ExpenseViewModel
import com.smartexpense.designsystems.ModernHeader
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(
    expenseId: Long,
    viewModel: ExpenseViewModel = koinViewModel(),
    onBackClick: () -> Unit,
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackBarHostState = remember { SnackbarHostState() }
    val photoPickerHelper = remember { PhotoPickerHelper() }
    val pickerLauncher = photoPickerHelper.rememberImagePickerLauncher { uri ->
        viewModel.onIntent(ExpenseIntent.ReceiptPhotoChanged(uri?.toString()))
    }

    var categoryExpanded by remember { mutableStateOf(false) }
    var paymentMethodExpanded by remember { mutableStateOf(false) }
    var expenseTypeExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(expenseId) {
        viewModel.onIntent(ExpenseIntent.LoadExpense(expenseId))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
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
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            ModernHeader(
                title = "Edit Expense",
                subtitle = currentExpense?.description ?: "Expense details",
                icon = Icons.Default.Receipt,
                onBackClick = onBackClick
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MoneyAmountField(
                            value = uiState.form.amount,
                            onValueChange = {
                                viewModel.onIntent(ExpenseIntent.AmountChanged(it))
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        ExposedDropdownMenuBox(
                            expanded = expenseTypeExpanded,
                            onExpandedChange = {
                                expenseTypeExpanded = !expenseTypeExpanded
                            }
                        ) {
                            OutlinedTextField(
                                value = uiState.form.expenseType.displayName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Expense Type") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = expenseTypeExpanded
                                    )
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = expenseTypeExpanded,
                                onDismissRequest = {
                                    expenseTypeExpanded = false
                                }
                            ) {
                                ExpenseType.entries.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type.displayName) },
                                        onClick = {
                                            viewModel.onIntent(
                                                ExpenseIntent.ExpenseTypeChanged(type)
                                            )
                                            expenseTypeExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        ExposedDropdownMenuBox(
                            expanded = categoryExpanded,
                            onExpandedChange = {
                                categoryExpanded = !categoryExpanded
                            }
                        ) {
                            OutlinedTextField(
                                value = uiState.categories
                                    .find { it.id == uiState.form.categoryId }
                                    ?.name
                                    ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = {
                                    Text("Category")
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = categoryExpanded
                                    )
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = {
                                    categoryExpanded = false
                                }
                            ) {
                                uiState.categories.forEach { category ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(category.name)
                                        },
                                        onClick = {
                                            viewModel.onIntent(
                                                ExpenseIntent.CategoryChanged(
                                                    category.id
                                                )
                                            )

                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        ExposedDropdownMenuBox(
                            expanded = paymentMethodExpanded,
                            onExpandedChange = {
                                paymentMethodExpanded = !paymentMethodExpanded
                            }
                        ) {
                            OutlinedTextField(
                                value = uiState.form.paymentMethod.displayName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Payment Method") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = paymentMethodExpanded
                                    )
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = paymentMethodExpanded,
                                onDismissRequest = {
                                    paymentMethodExpanded = false
                                }
                            ) {
                                PaymentMethod.entries.forEach { method ->
                                    DropdownMenuItem(
                                        text = { Text(method.displayName) },
                                        onClick = {
                                            viewModel.onIntent(
                                                ExpenseIntent.PaymentMethodChanged(method)
                                            )
                                            paymentMethodExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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

                        OutlinedTextField(
                            value = uiState.form.tags,
                            onValueChange = {
                                viewModel.onIntent(ExpenseIntent.TagsChanged(it))
                            },
                            label = {
                                Text("Tags (comma separated, e.g. work, trip)")
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = uiState.form.notes,
                            onValueChange = {
                                viewModel.onIntent(ExpenseIntent.NotesChanged(it))
                            },
                            label = {
                                Text("Notes")
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        ExpenseDateField(
                            dateMillis = uiState.form.date,
                            onDateSelected = {
                                viewModel.onIntent(ExpenseIntent.DateChanged(it))
                            }
                        )

                        ReceiptPhotoSection(
                            photoUri = uiState.form.receiptPhotoUri,
                            onPickPhoto = {
                                pickerLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            onRemovePhoto = {
                                viewModel.onIntent(ExpenseIntent.ReceiptPhotoChanged(null))
                            }
                        )
                    }
                }

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
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        if (uiState.form.isSaving) "Saving..." else "Save Changes",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Sync status: ${status.label()}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (status == ExpenseSyncStatus.FAILED || status == ExpenseSyncStatus.PENDING) {
            TextButton(onClick = onRetry) {
                Text("Sync Now")
            }
        }
    }
}
