package com.smartexpense.settings.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartexpense.designsystems.LivelyAmber
import com.smartexpense.designsystems.LivelyBlue
import com.smartexpense.designsystems.LivelyChoicePill
import com.smartexpense.designsystems.LivelyCyan
import com.smartexpense.designsystems.LivelyGreen
import com.smartexpense.designsystems.LivelyIconBadge
import com.smartexpense.designsystems.LivelyIndigo
import com.smartexpense.designsystems.LivelyOrange
import com.smartexpense.designsystems.LivelySectionCard
import com.smartexpense.designsystems.LivelyTeal
import com.smartexpense.designsystems.LivelyToggleRow
import com.smartexpense.designsystems.LivelyViolet
import com.smartexpense.designsystems.ModernHeader
import com.smartexpense.designsystems.ModernSectionHeader
import com.smartexpense.domain.model.AppTheme
import com.smartexpense.domain.model.BudgetAlertThresholds
import com.smartexpense.settings.intent.SettingsIntent
import com.smartexpense.settings.viewmodel.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ModernHeader(
            title = "Settings",
            subtitle = "Manage your SmartExpense preferences",
            icon = Icons.Default.Settings,
            onBackClick = onNavigateBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ModernSectionHeader(title = "Appearance")
            }

            item {
                LivelySectionCard(
                    accent = LivelyIndigo,
                    accentEnd = LivelyViolet,
                    title = "Theme",
                    subtitle = "Choose how Spendora looks",
                    icon = Icons.Default.Palette
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeTile(
                            label = "System",
                            icon = Icons.Default.BrightnessAuto,
                            selected = uiState.theme == AppTheme.SYSTEM,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.onIntent(
                                    SettingsIntent.ThemeChanged(AppTheme.SYSTEM)
                                )
                            }
                        )

                        ThemeTile(
                            label = "Light",
                            icon = Icons.Default.LightMode,
                            selected = uiState.theme == AppTheme.LIGHT,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.onIntent(
                                    SettingsIntent.ThemeChanged(AppTheme.LIGHT)
                                )
                            }
                        )

                        ThemeTile(
                            label = "Dark",
                            icon = Icons.Default.DarkMode,
                            selected = uiState.theme == AppTheme.DARK,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.onIntent(
                                    SettingsIntent.ThemeChanged(AppTheme.DARK)
                                )
                            }
                        )
                    }
                }
            }

            item {
                ModernSectionHeader(title = "Notifications")
            }

            item {
                LivelySectionCard(
                    accent = LivelyBlue,
                    accentEnd = LivelyCyan
                ) {
                    LivelyToggleRow(
                        title = "Expense Notifications",
                        subtitle = "Get notified about expenses and budgets",
                        icon = Icons.Default.NotificationsActive,
                        accent = LivelyBlue,
                        accentEnd = LivelyCyan,
                        checked = uiState.notificationsEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.onIntent(
                                SettingsIntent.NotificationsChanged(enabled)
                            )
                        }
                    )
                }
            }

            item {
                ModernSectionHeader(title = "Budget Alerts")
            }

            item {
                LivelySectionCard(
                    accent = LivelyOrange,
                    accentEnd = LivelyAmber,
                    title = "Alert thresholds",
                    subtitle = "Notify me when a budget reaches",
                    icon = Icons.Default.WarningAmber
                ) {
                    BudgetAlertThresholds.SUPPORTED.chunked(2).forEach { rowThresholds ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowThresholds.forEach { threshold ->
                                LivelyChoicePill(
                                    label = "$threshold%",
                                    selected = uiState.budgetAlertThresholds.contains(threshold),
                                    accent = LivelyOrange,
                                    accentEnd = LivelyAmber,
                                    enabled = uiState.notificationsEnabled,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        viewModel.onIntent(
                                            SettingsIntent.BudgetAlertThresholdsChanged(
                                                toggleThreshold(
                                                    uiState.budgetAlertThresholds,
                                                    threshold
                                                )
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LivelyIconBadge(
                            icon = Icons.Default.NotificationsActive,
                            accent = LivelyGreen,
                            accentEnd = LivelyTeal,
                            size = 28.dp,
                            shape = RoundedCornerShape(9.dp)
                        )
                        Text(
                            text = if (uiState.notificationsEnabled) {
                                "Alerts are active at the selected milestones."
                            } else {
                                "Turn on expense notifications to enable alerts."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeTile(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.background(
                        Brush.verticalGradient(
                            colors = listOf(LivelyIndigo, LivelyViolet)
                        )
                    )
                } else {
                    Modifier
                        .background(colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .border(
                            width = 1.dp,
                            color = LivelyIndigo.copy(alpha = 0.25f),
                            shape = shape
                        )
                }
            )
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .semantics {
                role = Role.RadioButton
            }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = if (selected) Color.White else LivelyIndigo
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

private fun toggleThreshold(
    current: Set<Int>,
    threshold: Int
): Set<Int> {
    return if (current.contains(threshold)) {
        current - threshold
    } else {
        current + threshold
    }
}
