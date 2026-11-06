package com.smartexpense.feature.expenses.ui

import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpense.designsystems.ModernHeader
import com.smartexpense.domain.model.ExpenseType
import com.smartexpense.domain.model.PaymentMethod
import com.smartexpense.feature.expenses.effect.ExpenseEffect
import com.smartexpense.feature.expenses.intent.ExpenseIntent
import com.smartexpense.feature.expenses.viewmodel.ExpenseViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    viewModel: ExpenseViewModel = koinViewModel(),
    onBackClick: () -> Unit,
) {

    val keyboardController = LocalSoftwareKeyboardController.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var categoryExpanded by remember { mutableStateOf(false) }
    var paymentMethodExpanded by remember { mutableStateOf(false) }
    var expenseTypeExpanded by remember { mutableStateOf(false) }
    val photoPickerHelper = remember { PhotoPickerHelper() }
    val pickerLauncher = photoPickerHelper.rememberImagePickerLauncher { uri ->
        viewModel.onIntent(ExpenseIntent.ReceiptPhotoChanged(uri?.toString()))
    }

    val snackBarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ExpenseEffect.ExpenseAdded -> {
                    snackBarHostState.showSnackbar(
                        message = "Expense Added Successfully"
                    )
                }

                ExpenseEffect.NavigateToAddExpense -> {
                }

                is ExpenseEffect.ShowError -> {
                    snackBarHostState.showSnackbar(
                        message = effect.message
                    )
                }

                ExpenseEffect.ExpenseUpdated -> {
                }

                ExpenseEffect.ExpenseDeleted -> {
                    snackBarHostState.showSnackbar(
                        message = "Expense deleted successfully!"
                    )
                }

                ExpenseEffect.SyncRetryRequested -> {
                    snackBarHostState.showSnackbar(
                        message = "Sync retry requested"
                    )
                }
            }
        }
    }

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
                title = "Add Expense",
                subtitle = "Record a new expense",
                icon = Icons.Default.Add,
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

                Button(
                    onClick = {
                        viewModel.onIntent(
                            intent = ExpenseIntent.AddExpenses
                        )

                        keyboardController?.hide()
                    },
                    enabled = !uiState.form.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (uiState.form.isSaving) "Saving..." else "Add Expense",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
