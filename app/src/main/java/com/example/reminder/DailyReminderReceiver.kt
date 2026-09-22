package com.example.reminder

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.FinanceApplication
import com.example.MainActivity
import com.example.R

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        showReminderNotification(context)

        // Reschedule for the next day
        ReminderManager.rescheduleNextDay(context)
    }

    private fun showReminderNotification(context: Context) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, FinanceApplication.CHANNEL_REMINDER_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Nhắc nhở chi tiêu hôm nay 💰")
            .setContentText("Bạn đã ghi lại các khoản thu chi hôm nay chưa? Dành 1 phút cập nhật nhé!")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Bạn đã ghi lại các khoản thu chi hôm nay chưa? Dành 1 phút để cập nhật sổ chi tiêu và kiểm soát tài chính cá nhân hiệu quả nhé!"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(1001, notification)
        } catch (_: SecurityException) {
            // Notification permission might not be granted yet
        }
    }
}
