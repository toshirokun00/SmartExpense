package com.smartexpense.data.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartexpense.core.common.toPesoAmount
import com.smartexpense.database.dao.BillDao
import com.smartexpense.domain.repository.AuthRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Daily worker that posts a reminder for unpaid bills that fall inside their
 * per-bill reminder window (e.g. 3 days before the due date). Recurring bills
 * roll over to their next due date when paid, so they are naturally picked up
 * again for their next cycle. Paused and skipped bills are ignored.
 */
class BillReminderWorker(
    appContext: Context,
    params: WorkerParameters,
    private val billDao: BillDao,
    private val authRepository: AuthRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val userId = authRepository.getCurrentUserId() ?: return Result.success()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)

        val bills = billDao.getBillsForUser(userId).first()
        val upcoming = bills.filter { bill ->
            if (bill.isPaid || bill.skipped || !bill.isActive) return@filter false
            val due = Instant.ofEpochMilli(bill.dueDate).atZone(zone).toLocalDate()
            val windowEnd = today.plusDays(bill.reminderDaysBefore.coerceAtLeast(0).toLong())
            !due.isBefore(today) && !due.isAfter(windowEnd)
        }
        if (upcoming.isEmpty()) return Result.success()

        val manager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Bill reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }

        upcoming.forEach { bill ->
            val due = Instant.ofEpochMilli(bill.dueDate).atZone(zone).toLocalDate()
            val days = ChronoUnit.DAYS.between(today, due).toInt()
            val timing = when (days) {
                0 -> "Due today"
                1 -> "Due tomorrow"
                else -> "Due in $days days"
            }
            val amountLabel = if (bill.amountEstimated) "est. " else ""
            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(bill.name)
                .setContentText("$timing • $amountLabel${bill.amount.toPesoAmount()}")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            manager.notify(bill.id.toInt(), notification)
        }
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "bill_reminders"
        const val WORK_NAME = "bill_reminder_daily"
    }
}
