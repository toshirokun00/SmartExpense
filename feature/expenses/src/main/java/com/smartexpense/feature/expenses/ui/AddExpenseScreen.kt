package com.smartexpense.feature.expenses.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    var categoryExpanded by remember {
        mutableStateOf(false)
    }

     val selectedCategory = uiState.categories.find {
         it.id == uiState.form.categoryId
     }

    val snackBarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when(effect) {
                ExpenseEffect.ExpenseAdded -> {
                    snackBarHostState.showSnackbar(
                        message = "Expense Added Successfully"
                    )
                }

                ExpenseEffect.NavigateToAddExpense -> {

                }
                is ExpenseEffect.ShowError ->  {
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
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Add Expense")
                },
                navigationIcon = {
                    IconButton(onClick = {
                        onBackClick()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "back"
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState)
        }
    ) { innnerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innnerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.form.amount,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                onValueChange = {
                    viewModel.onIntent(ExpenseIntent.AmountChanged(it))
                },
                label = {
                    Text("Amount")
                }
            )

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

            OutlinedTextField(
                value = uiState.form.description,
                onValueChange = {
                    viewModel.onIntent(ExpenseIntent.DescriptionChanged(it))
                },
                label = {
                    Text("Description")
                }
            )


            Button(
                onClick = {
                    viewModel.onIntent(
                        intent = ExpenseIntent.AddExpenses)

                    keyboardController?.hide()
                },
            ) {
                Text(text = "Add Expense")
            }

        }
    }

}