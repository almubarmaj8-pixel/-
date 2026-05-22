package com.example.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.receiver.AlarmReceiver
import kotlinx.coroutines.flow.Flow

class AlarmRepository(
    private val context: Context,
    private val alarmDao: AlarmDao
) {
    val allAlarms: Flow<List<Alarm>> = alarmDao.getAllAlarms()
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    suspend fun getAlarmById(id: Int): Alarm? = alarmDao.getAlarmById(id)

    suspend fun insertAlarm(alarm: Alarm): Long {
        val id = alarmDao.insertAlarm(alarm)
        val savedAlarm = alarm.copy(id = id.toInt())
        if (savedAlarm.isEnabled) {
            scheduleAlarm(savedAlarm)
        }
        return id
    }

    suspend fun updateAlarm(alarm: Alarm) {
        alarmDao.updateAlarm(alarm)
        if (alarm.isEnabled) {
            scheduleAlarm(alarm)
        } else {
            cancelAlarm(alarm)
        }
    }

    suspend fun deleteAlarm(alarm: Alarm) {
        cancelAlarm(alarm)
        alarmDao.deleteAlarm(alarm)
    }

    suspend fun toggleAlarm(alarm: Alarm) {
        val newState = !alarm.isEnabled
        alarmDao.updateEnabledState(alarm.id, newState)
        val updatedAlarm = alarm.copy(isEnabled = newState)
        if (newState) {
            scheduleAlarm(updatedAlarm)
        } else {
            cancelAlarm(updatedAlarm)
        }
    }

    fun scheduleAlarm(alarm: Alarm) {
        if (alarmManager == null) return
        val triggerTime = alarm.getNextTriggerMillis()

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            putExtra("ALARM_LABEL", alarm.label)
            putExtra("ALARM_HOUR", alarm.hour)
            putExtra("ALARM_MINUTE", alarm.minute)
            putExtra("ALARM_PUZZLE_TYPE", alarm.puzzleType)
            putExtra("ALARM_PUZZLE_DIFFICULTY", alarm.puzzleDifficulty)
            putExtra("ALARM_VIBRATE", alarm.isVibrate)
            putExtra("ALARM_GENTLE", alarm.isGentleWakeUp)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
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
            Log.d("AlarmRepository", "Scheduled alarm ${alarm.id} at $triggerTime")
        } catch (e: SecurityException) {
            // Fallback for security exception (if schedule exact alarm requires special permission not granted)
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
            Log.w("AlarmRepository", "Fallback to standard non-exact alarm due to permission constraints", e)
        }
    }

    fun cancelAlarm(alarm: Alarm) {
        if (alarmManager == null) return
        val intent = Intent(context, AlarmReceiver::class.java)
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            flags
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("AlarmRepository", "Canceled alarm ${alarm.id}")
        }
    }
}
