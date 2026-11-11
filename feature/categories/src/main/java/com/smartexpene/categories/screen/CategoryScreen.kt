package com.smartexpene.categories.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpene.categories.effect.CategoryEffect
import com.smartexpene.categories.intent.CategoryIntent
import com.smartexpene.categories.viewmodel.CategoryViewModel
import com.smartexpense.designsystems.LivelyGradientButton
import com.smartexpense.designsystems.LivelyIndigo
import com.smartexpense.designsystems.LivelyInfoRow
import com.smartexpense.designsystems.LivelyPink
import com.smartexpense.designsystems.LivelySectionCard
import com.smartexpense.designsystems.LivelyViolet
import com.smartexpense.designsystems.ModernEmptyState
import com.smartexpense.designsystems.ModernHeader
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
        },
        // The header draws behind the status bar and handles its own inset.
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            ModernHeader(
                title = if (uiState.editingCategoryId != null) "Edit Category" else "Categories",
                subtitle = "Manage your expense categories",
                icon = Icons.Default.Category,
                onBackClick = onBackClick
            )

            LivelySectionCard(
                accent = LivelyIndigo,
                accentEnd = LivelyViolet,
                title = if (uiState.editingCategoryId != null) "Edit Category" else "New Category",
                subtitle = "Organize your spending",
                icon = Icons.Default.Category,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
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
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LivelyIndigo,
                        unfocusedBorderColor = LivelyIndigo.copy(alpha = 0.28f),
                        focusedLabelColor = LivelyIndigo,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = LivelyIndigo,
                        focusedContainerColor = LivelyIndigo.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(14.dp)
                )

                if (uiState.editingCategoryId == null) {
                    LivelyGradientButton(
                        text = "Add Category",
                        accent = LivelyIndigo,
                        accentEnd = LivelyViolet,
                        icon = Icons.Default.Add,
                        onClick = {
                            viewModel.onIntent(CategoryIntent.AddCategory)
                        }
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LivelyGradientButton(
                            text = "Update",
                            accent = LivelyIndigo,
                            accentEnd = LivelyViolet,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.onIntent(CategoryIntent.UpdateCategory)
                            }
                        )

                        OutlinedButton(
                            onClick = {
                                viewModel.onIntent(
                                    CategoryIntent.CancelEdit
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
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
                        val isEditing = uiState.editingCategoryId == category.id
                        val accent = if (isEditing) LivelyIndigo else LivelyViolet
                        val accentEnd = if (isEditing) LivelyViolet else LivelyPink

                        LivelySectionCard(
                            accent = accent,
                            accentEnd = accentEnd,
                            modifier = Modifier.fillMaxWidth(),
                            compact = true
                        ) {
                            LivelyInfoRow(
                                title = category.name,
                                subtitle = if (isEditing) "Editing now" else "Tap to edit",
                                icon = Icons.Default.Category,
                                accent = accent,
                                accentEnd = accentEnd,
                                onClick = {
                                    viewModel.onIntent(
                                        CategoryIntent.LoadCategory(category.id)
                                    )
                                },
                                trailing = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        TextButton(
                                            onClick = {
                                                viewModel.onIntent(
                                                    CategoryIntent.LoadCategory(category.id)
                                                )
                                            },
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = LivelyIndigo
                                            )
                                        ) {
                                            Text(
                                                "Edit",
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        TextButton(
                                            onClick = {
                                                viewModel.onIntent(
                                                    CategoryIntent.RequestDeleteCategory(
                                                        category.id
                                                    )
                                                )
                                            },
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error
                                            )
                                        ) {
                                            Text(
                                                "Delete",
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                },
                                compact = true
                            )
                        }
                    }
                }
            }
        }
    }
}
