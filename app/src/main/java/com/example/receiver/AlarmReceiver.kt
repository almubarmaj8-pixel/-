package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.Alarm
import com.example.data.AlarmDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        if (alarmId == -1) return

        Log.d("AlarmReceiver", "Alarm triggered with ID: $alarmId")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AlarmDatabase.getDatabase(context)
                val alarm = database.alarmDao.getAlarmById(alarmId)
                if (alarm != null && alarm.isEnabled) {
                    // Update state holder
                    AlarmStateHolder.setRingingAlarm(alarm)

                    // Start sound and vibration
                    AlarmSoundManager.startRinging(context, alarm.isVibrate)

                    // Show notification
                    showAlarmNotification(context, alarm)
                }
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Error retrieving alarm details", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showAlarmNotification(context: Context, alarm: Alarm) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "smart_alarm_ring_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "رنين المنبه الذكي",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات رنين المنبه الذكي"
                enableLights(true)
                enableVibration(alarm.isVibrate)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Action when clicking the notification directly (opens the app to solving the active alarm page)
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_RINGING", alarm.id)
        }
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            alarm.id,
            mainIntent,
            flags
        )

        // Dismiss action
        val dismissIntent = Intent(context, AlarmDismissReceiver::class.java).apply {
            action = "DISMISS"
            putExtra("ALARM_ID", alarm.id)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            dismissIntent,
            flags
        )

        // Snooze action
        val snoozeIntent = Intent(context, AlarmDismissReceiver::class.java).apply {
            action = "SNOOZE"
            putExtra("ALARM_ID", alarm.id)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id + 10000, // distinct request code
            snoozeIntent,
            flags
        )

        val title = if (alarm.label.isNotEmpty()) alarm.label else "حان وقت الاستيقاظ!"
        val contentText = "الساعة الآن ${alarm.getFormattedTime()}"

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "إيقاف المنبه",
                dismissPendingIntent
            )
            .addAction(
                android.R.drawable.ic_popup_sync,
                "غفوة (${alarm.snoozeTimeMinutes} د)",
                snoozePendingIntent
            )

        notificationManager.notify(alarm.id, builder.build())
    }
}
