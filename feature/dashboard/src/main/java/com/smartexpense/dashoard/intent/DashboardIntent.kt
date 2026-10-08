package com.smartexpense.dashoard.intent

sealed interface DashboardIntent {

    data object LoadDashboard : DashboardIntent

    data object RefreshDashboard : DashboardIntent
}