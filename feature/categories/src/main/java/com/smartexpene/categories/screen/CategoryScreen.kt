package com.smartexpene.categories.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpene.categories.effect.CategoryEffect
import com.smartexpene.categories.intent.CategoryIntent
import com.smartexpene.categories.viewmodel.CategoryViewModel
import com.smartexpense.designsystems.ModernEmptyState
import com.smartexpense.designsystems.ModernHeader
import com.smartexpense.designsystems.ModernListItemCard
import com.smartexpense.designsystems.ModernSectionHeader
import com.smartexpense.domain.model.Category
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    viewModel: CategoryViewModel = koinViewModel(),
    onBackClick: () -> Unit,
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->

            when (effect) {
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

    if (screenState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = {
                viewModel.onIntent(CategoryIntent.CancelDeleteCategory)
            },
            title = {
                Text("Delete Category")
            },
            text = {
                Text("Are you sure you want to delete this category?")
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

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ModernHeader(
                title = if (uiState.editingCategoryId != null) "Edit Category" else "Categories",
                subtitle = "Manage your expense categories",
                icon = Icons.Default.Category,
                onBackClick = onBackClick
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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

                    if (uiState.editingCategoryId == null) {
                        Button(
                            onClick = {
                                viewModel.onIntent(CategoryIntent.AddCategory)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Add Category")
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.onIntent(CategoryIntent.UpdateCategory)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Update")
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.onIntent(
                                        CategoryIntent.CancelEdit
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    ModernSectionHeader(
                        title = if (uiState.categories.isEmpty()) "No Categories" else "Your Categories"
                    )
                }

                if (uiState.categories.isEmpty()) {
                    item {
                        ModernEmptyState(
                            icon = Icons.Default.Category,
                            title = "No categories yet",
                            subtitle = "Add a category above to get started."
                        )
                    }
                } else {
                    items(
                        items = uiState.categories,
                        key = { it.id }
                    ) { category ->
                        ModernListItemCard(
                            avatarIcon = Icons.Default.Category,
                            avatarColor = if (uiState.editingCategoryId == category.id) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.secondary
                            },
                            title = category.name,
                            subtitle = null,
                            trailing = {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            viewModel.onIntent(
                                                CategoryIntent.LoadCategory(category.id)
                                            )
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.onIntent(
                                                CategoryIntent.RequestDeleteCategory(
                                                    category.id
                                                )
                                            )
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
