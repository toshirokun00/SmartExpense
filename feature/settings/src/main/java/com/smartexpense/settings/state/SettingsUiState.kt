package com.smartexpense.settings.state

import com.smartexpense.domain.model.AppTheme
import com.smartexpense.domain.model.BudgetAlertThresholds

data class SettingsUiState(
    val theme: AppTheme = AppTheme.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val budgetAlertThresholds: Set<Int> = BudgetAlertThresholds.DEFAULT,
    val isLoading: Boolean = false,
    val error: String? = null
)
