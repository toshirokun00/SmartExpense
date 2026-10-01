package com.smartexpene.categories.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpene.categories.effect.CategoryEffect
import com.smartexpene.categories.intent.CategoryIntent
import com.smartexpene.categories.viewmodel.CategoryViewModel
import com.smartexpense.domain.model.Category
import org.koin.androidx.compose.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    viewModel: CategoryViewModel = koinViewModel(),
    onBackClick : () -> Unit,
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    val snackbarHostState  = remember {
        SnackbarHostState()
    }

    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->

            when(effect) {
                CategoryEffect.CategoryAdded -> {
                    snackbarHostState.showSnackbar("Category added successfully")
                }
                CategoryEffect.CategoryDeleted -> {
                    snackbarHostState.showSnackbar("Category deleted successfully")
                }
                CategoryEffect.CategoryUpdated -> {
                    snackbarHostState.showSnackbar("Category updated successfully")
                }
                is CategoryEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Categories")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null)
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    )
    { innerPadding ->

        Column(
            modifier = Modifier.fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = {
                    viewModel.onIntent(CategoryIntent.NameChanged(it))
                },
                label = {
                    Text("Category name")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = {

                    if (uiState.editingCategoryId == null) {
                        viewModel.onIntent(CategoryIntent.AddCategory)
                    } else {
                        viewModel.onIntent(CategoryIntent.UpdateCategory)
                    }

                    keyboardController?.hide()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = if (uiState.editingCategoryId == null) {
                    "Add Category"
                } else {
                    "Update Category"
                })
            }
            if (uiState.editingCategoryId != null) {
                OutlinedButton(
                    onClick = {
                        viewModel.onIntent(
                            CategoryIntent.CancelEdit
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }

            HorizontalDivider()

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items  = uiState.categories,
                    key = { it.id}
                ) { category ->

                    CategoryItem(
                        category,
                        onEditClick = {
                            viewModel.onIntent(CategoryIntent.UpdateCategory)
                        },
                        onDeleteClick = {
                            viewModel.onIntent(CategoryIntent.RequestDeleteCategory(category.id))
                        }
                    )

                }
            }
        }
    }

    if (screenState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = {
                viewModel.onIntent(CategoryIntent.CancelDeleteCategory)
            },
            title = {
                Text("Delete Category")
            },
            text = {
                Text("Are yo sure you want to delete this category?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onIntent(CategoryIntent.ConfirmDeleteCategory)
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.onIntent(CategoryIntent.CancelDeleteCategory)
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

}


@Composable
private fun CategoryItem(
    category: Category,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = category.name,
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = onEditClick
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit"
            )
        }

        IconButton(
            onClick = onDeleteClick
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete"
            )
        }
    }
}