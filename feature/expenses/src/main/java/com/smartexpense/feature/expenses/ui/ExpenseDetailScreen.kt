package com.smartexpense.feature.expenses.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

                }

                ExpenseEffect.ExpenseDeleted -> Unit
            }

        }
    }

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
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            OutlinedTextField(
                value = uiState.form.amount,
                onValueChange = {
                    viewModel.onIntent(ExpenseIntent.AmountChanged(it))
                },
                label = {
                    Text("Amount")
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

            Button(
                onClick = {
                  viewModel.onIntent(ExpenseIntent.UpdateExpense(id = expenseId))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Changes")
            }
        }

    }
}