package com.example.receiver

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.MainActivity
import com.example.data.AlarmDatabase
import com.example.data.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        val action = intent.action ?: return

        Log.d("AlarmDismissReceiver", "Action $action received for alarm: $alarmId")

        // Stop the ringer immediately
        AlarmSoundManager.stopRinging()
        AlarmStateHolder.setRingingAlarm(null)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(alarmId)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AlarmDatabase.getDatabase(context)
                val repository = AlarmRepository(context, db.alarmDao)
                val alarm = db.alarmDao.getAlarmById(alarmId)

                if (alarm != null) {
                    if (action == "DISMISS") {
                        if (alarm.daysToRepeat.isEmpty()) {
                            db.alarmDao.updateEnabledState(alarm.id, false)
                        } else {
                            repository.scheduleAlarm(alarm)
                        }

                        // If alarm has an active puzzle challenge, force launch MainActivity to present UI
                        if (alarm.puzzleType != "NONE") {
                            val activityIntent = Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                putExtra("OPEN_RINGING", alarm.id)
                            }
                            context.startActivity(activityIntent)
                        }
                    } else if (action == "SNOOZE") {
                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                        if (alarmManager != null) {
                            val triggerTime = Calendar.getInstance().apply {
                                add(Calendar.MINUTE, alarm.snoozeTimeMinutes)
                            }.timeInMillis

                            val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
                                putExtra("ALARM_ID", alarm.id)
                            }

                            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            } else {
                                PendingIntent.FLAG_UPDATE_CURRENT
                            }

                            val pendingIntent = PendingIntent.getBroadcast(
                                context,
                                alarm.id,
                                snoozeIntent,
                                flags
                            )

                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    alarmManager.setExactAndAllowWhileIdle(
                                        AlarmManager.RTC_WAKEUP,
                                        triggerTime,
                                        pendingIntent
                                    )
                                } else {
                                    alarmManager.setExact(
                                        AlarmManager.RTC_WAKEUP,
                                        triggerTime,
                                        pendingIntent
                                    )
                                }
                            } catch (e: SecurityException) {
                                alarmManager.set(
                                    AlarmManager.RTC_WAKEUP,
                                    triggerTime,
                                    pendingIntent
                                )
                            }
                            Log.d("AlarmDismissReceiver", "Alarm ${alarm.id} snoozed for ${alarm.snoozeTimeMinutes} mins")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AlarmDismissReceiver", "Error while handling alarm event", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
