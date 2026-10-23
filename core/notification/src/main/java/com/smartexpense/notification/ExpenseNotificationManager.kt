package com.smartexpense.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class ExpenseNotificationManager(
    private val context : Context
) {

    companion object {
        private const val CHANNEL_ID = "expense_notifications"
        private const val CHANNEL_NAME = "Expense notifications"
        private const val NOTIFICATION_ID = 1001

        /**
         * Budget alerts use distinct ids so multiple budgets/thresholds can be
         * surfaced at the same time without overwriting each other.
         */
        private const val BUDGET_NOTIFICATION_ID_BASE = 2000
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications about expense activity"
            }

            val manager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            manager.createNotificationChannel(channel)
        }
    }


    fun showExpenseAddedNotification(
        description: String,
        amount: String
    ): Boolean {
        return notify(
            notificationId = NOTIFICATION_ID,
            title = "Expense added",
            message = "$description — $amount"
        )
    }

    /**
     * Displays a budget utilization alert. The [budgetId] and [threshold] are used
     * to derive a stable notification id so repeated deliveries for the same budget
     * and threshold reuse the same notification slot rather than stacking.
     */
    fun showBudgetAlertNotification(
        budgetId: Long,
        threshold: Int,
        title: String,
        message: String
    ): Boolean {
        return notify(
            notificationId = budgetNotificationId(budgetId, threshold),
            title = title,
            message = message
        )
    }

    private fun budgetNotificationId(budgetId: Long, threshold: Int): Int {
        return BUDGET_NOTIFICATION_ID_BASE + (budgetId.hashCode() * 31 + threshold)
    }

    private fun notify(
        notificationId: Int,
        title: String,
        message: String
    ): Boolean {
        if (!canPostNotifications()) {
            return false
        }

        return try {
            val notification = NotificationCompat.Builder(
                context,
                CHANNEL_ID
            )
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat
                .from(context)
                .notify(notificationId, notification)

            true
        } catch (_: SecurityException) {
            false
        }
    }

    private fun canPostNotifications(): Boolean {
        return !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED)
    }
}
