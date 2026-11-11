package com.smartexpense.feature.expenses.ui

import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
        modifier = Modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState)
        },
        // The header draws behind the status bar and handles its own inset.
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        ExpenseFormContent(
            title = "Add Expense",
            subtitle = "Record a new expense",
            icon = Icons.Default.Add,
            form = uiState.form,
            categories = uiState.categories,
            submitLabel = if (uiState.form.isSaving) {
                "Saving..."
            } else {
                "Add Expense"
            },
            onBackClick = onBackClick,
            onAmountChanged = {
                viewModel.onIntent(ExpenseIntent.AmountChanged(it))
            },
            onExpenseTypeChanged = {
                viewModel.onIntent(ExpenseIntent.ExpenseTypeChanged(it))
            },
            onCategoryChanged = {
                viewModel.onIntent(ExpenseIntent.CategoryChanged(it))
            },
            onPaymentMethodChanged = {
                viewModel.onIntent(ExpenseIntent.PaymentMethodChanged(it))
            },
            onDescriptionChanged = {
                viewModel.onIntent(ExpenseIntent.DescriptionChanged(it))
            },
            onMerchantChanged = {
                viewModel.onIntent(ExpenseIntent.MerchantChanged(it))
            },
            onTagsChanged = {
                viewModel.onIntent(ExpenseIntent.TagsChanged(it))
            },
            onNotesChanged = {
                viewModel.onIntent(ExpenseIntent.NotesChanged(it))
            },
            onDateChanged = {
                viewModel.onIntent(ExpenseIntent.DateChanged(it))
            },
            onPickPhoto = {
                pickerLauncher.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            },
            onRemovePhoto = {
                viewModel.onIntent(ExpenseIntent.ReceiptPhotoChanged(null))
            },
            onSubmit = {
                viewModel.onIntent(
                    intent = ExpenseIntent.AddExpenses
                )
                keyboardController?.hide()
            },
            modifier = Modifier.padding(innerPadding)
        )
    }
}
