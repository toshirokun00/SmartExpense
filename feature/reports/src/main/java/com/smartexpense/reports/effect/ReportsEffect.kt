package com.smartexpense.reports.effect

sealed interface ReportsEffect {
    data class ShowError(val message: String) : ReportsEffect

    data class ShowMessage(val message: String) : ReportsEffect

    data class LaunchSaveDocument(
        val fileName: String,
        val mimeType: String
    ) : ReportsEffect

    data class LaunchShare(
        val fileUri: String,
        val mimeType: String,
        val title: String
    ) : ReportsEffect
}
