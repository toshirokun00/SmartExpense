package com.portfolio.settings.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.portfolio.settings.effect.SettingsEffect
import com.portfolio.settings.intent.SettingsIntent
import com.portfolio.settings.viewmodel.SettingsViewModel
import com.smartexpense.domain.model.AppTheme
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToSignIn: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showSignOutDialog by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when(effect) {
                SettingsEffect.NavigateToSignIn -> {
                    onNavigateToSignIn()
                }
                is SettingsEffect.ShowError -> {
                    errorMessage = effect.message
                }
            }

        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = {
                showSignOutDialog = false
            },
            title = {
                Text("Sign out?")
            },
            text = {
                Text("Are you sure you want to sign out of SmartExpense?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.onIntent(SettingsIntent.SignOut)
                    }
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = {
                errorMessage = null
            },
            title = {
                Text("Sign Out Failed")
            },
            text = {
                Text(message)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        errorMessage = null
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Settings")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack
                    ) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text("Appearance")

            ThemeOption(
                title = "System default",
                selected = uiState.theme == AppTheme.SYSTEM,
                onClick = {
                    viewModel.onIntent(
                        SettingsIntent.ThemeChanged(AppTheme.SYSTEM)
                    )
                }
            )

            ThemeOption(
                title = "Light",
                selected = uiState.theme == AppTheme.LIGHT,
                onClick = {
                    viewModel.onIntent(
                        SettingsIntent.ThemeChanged(AppTheme.LIGHT)
                    )
                }
            )

            ThemeOption(
                title = "Dark",
                selected = uiState.theme == AppTheme.DARK,
                onClick = {
                    viewModel.onIntent(
                        SettingsIntent.ThemeChanged(AppTheme.DARK)
                    )
                }
            )

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Notifications")

                    Text(
                        text = "Enable expense notifications"
                    )
                }

                Switch(
                    checked = uiState.notificationsEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.onIntent(
                            SettingsIntent.NotificationsChanged(
                                enabled
                            )
                        )
                    }
                )
            }

            Button(
                onClick = {
                    showSignOutDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign Out")
            }
        }
    }
}

@Composable
private fun ThemeOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .semantics {
                role = Role.RadioButton
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )

        Text(
            text = title,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}