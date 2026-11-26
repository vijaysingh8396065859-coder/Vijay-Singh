package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import java.util.Calendar

/**
 * Roz subah aur shaam doodh collection ki yaad dilane wale reminders.
 * Alarm ek baar ka lagta hai; receiver notification dikhakar agle din ka
 * alarm khud laga deta hai, isliye koi server nahi chahiye.
 */
object CollectionReminders {
    const val CHANNEL_ID = "collection_reminders"
    const val MORNING_REQUEST = 101
    const val EVENING_REQUEST = 102
    const val MORNING_HOUR = 5
    const val MORNING_MINUTE = 30
    const val EVENING_HOUR = 17
    const val EVENING_MINUTE = 30

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "दूध Collection Reminder",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Subah aur shaam doodh lene ki yaad"
        }
        manager.createNotificationChannel(channel)
    }

    fun scheduleAll(context: Context) {
        schedule(context, MORNING_REQUEST, MORNING_HOUR, MORNING_MINUTE)
        schedule(context, EVENING_REQUEST, EVENING_HOUR, EVENING_MINUTE)
    }

    fun schedule(context: Context, requestCode: Int, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(ReminderReceiver.EXTRA_REQUEST_CODE, requestCode)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                next.timeInMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Alarm permission na mile to bhi app par koi asar nahi padna chahiye
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val requestCode = intent.getIntExtra(
            EXTRA_REQUEST_CODE,
            CollectionReminders.MORNING_REQUEST
        )
        val isMorning = requestCode == CollectionReminders.MORNING_REQUEST
        val title = if (isMorning) "🥛 Subah ho gayi!" else "🥛 Shaam ho gayi!"
        val text = if (isMorning) {
            "Doodh lene chalo — subah ki collection ka samay ho gaya hai।"
        } else {
            "Doodh lene chalo — shaam ki collection ka samay ho gaya hai।"
        }

        val openIntent = PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CollectionReminders.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_drop)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(requestCode, notification)
        } catch (e: SecurityException) {
            // Notification permission nahi mili — chup chaap aage badho
        }

        // Agle din ke liye wahi alarm dobara lagao
        if (isMorning) {
            CollectionReminders.schedule(
                context,
                CollectionReminders.MORNING_REQUEST,
                CollectionReminders.MORNING_HOUR,
                CollectionReminders.MORNING_MINUTE
            )
        } else {
            CollectionReminders.schedule(
                context,
                CollectionReminders.EVENING_REQUEST,
                CollectionReminders.EVENING_HOUR,
                CollectionReminders.EVENING_MINUTE
            )
        }
    }

    companion object {
        const val EXTRA_REQUEST_CODE = "extra_request_code"
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CollectionReminders.createChannel(context)
            CollectionReminders.scheduleAll(context)
        }
    }
}
