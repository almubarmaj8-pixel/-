package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Alarm
import com.example.data.AlarmDatabase
import com.example.data.AlarmRepository
import com.example.receiver.AlarmStateHolder
import com.example.receiver.AlarmSoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AlarmDatabase.getDatabase(application)
    private val repository = AlarmRepository(application, database.alarmDao)

    val alarms: StateFlow<List<Alarm>> = repository.allAlarms
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val ringingAlarm: StateFlow<Alarm?> = AlarmStateHolder.ringingAlarm
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Countdown message for the next active alarm
    private val _nextAlarmText = MutableStateFlow<String?>("لا يوجد منبهات نشطة")
    val nextAlarmText: StateFlow<String?> = _nextAlarmText

    // Simulated local statistics for early wake ups
    private val _statsWakeups = MutableStateFlow(8)
    val statsWakeups: StateFlow<Int> = _statsWakeups

    private val _statsSnoozes = MutableStateFlow(3)
    val statsSnoozes: StateFlow<Int> = _statsSnoozes

    private val _statsStreak = MutableStateFlow(4)
    val statsStreak: StateFlow<Int> = _statsStreak

    init {
        // Update countdown text every 10 seconds
        viewModelScope.launch {
            while (true) {
                updateNextAlarmCountdown()
                delay(10000)
            }
        }
        
        // Also update when alarms state emits
        viewModelScope.launch {
            alarms.collect {
                updateNextAlarmCountdown()
            }
        }
    }

    private fun updateNextAlarmCountdown() {
        val activeAlarms = alarms.value.filter { it.isEnabled }
        if (activeAlarms.isEmpty()) {
            _nextAlarmText.value = "لا يوجد منبهات نشطة حالياً"
            return
        }

        var nextAlarm: Alarm? = null
        var nextTriggerTime = Long.MAX_VALUE

        for (alarm in activeAlarms) {
            val trigger = alarm.getNextTriggerMillis()
            if (trigger < nextTriggerTime) {
                nextTriggerTime = trigger
                nextAlarm = alarm
            }
        }

        if (nextAlarm != null) {
            val diffMs = nextTriggerTime - System.currentTimeMillis()
            if (diffMs <= 0) {
                _nextAlarmText.value = "المنبه ينطلق الآن"
                return
            }

            val diffSeconds = diffMs / 1000
            val diffMinutes = (diffSeconds / 60) % 60
            val diffHours = (diffSeconds / 3600) % 24
            val diffDays = diffSeconds / 86400

            val builder = StringBuilder("المنبه التالي خلال: ")
            if (diffDays > 0) {
                builder.append("$diffDays يوم و ")
            }
            if (diffHours > 0) {
                builder.append("$diffHours ساعة و ")
            }
            builder.append("$diffMinutes دقيقة")
            _nextAlarmText.value = builder.toString()
        } else {
            _nextAlarmText.value = "لا يوجد منبهات نشطة"
        }
    }

    fun addAlarm(
        hour: Int, 
        minute: Int, 
        daysToRepeat: String, 
        label: String, 
        isVibrate: Boolean, 
        isGentle: Boolean, 
        puzzleType: String, 
        difficulty: String, 
        snoozeMins: Int
    ) {
        viewModelScope.launch {
            val alarm = Alarm(
                hour = hour,
                minute = minute,
                isEnabled = true,
                label = label,
                daysToRepeat = daysToRepeat,
                isVibrate = isVibrate,
                isGentleWakeUp = isGentle,
                puzzleType = puzzleType,
                puzzleDifficulty = difficulty,
                snoozeTimeMinutes = snoozeMins
            )
            repository.insertAlarm(alarm)
            updateNextAlarmCountdown()
        }
    }

    fun updateAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.updateAlarm(alarm)
            updateNextAlarmCountdown()
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.deleteAlarm(alarm)
            updateNextAlarmCountdown()
        }
    }

    fun toggleAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.toggleAlarm(alarm)
            updateNextAlarmCountdown()
        }
    }

    fun dismissRinging(alarmId: Int) {
        viewModelScope.launch {
            AlarmSoundManager.stopRinging()
            AlarmStateHolder.setRingingAlarm(null)
            
            val notificationManager = getApplication<Application>().getSystemService(Application.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancel(alarmId)

            val alarm = repository.getAlarmById(alarmId)
            if (alarm != null) {
                if (alarm.daysToRepeat.isEmpty()) {
                    repository.updateAlarm(alarm.copy(isEnabled = false))
                } else {
                    repository.scheduleAlarm(alarm)
                }
                increaseWakeupStats()
            }
        }
    }

    fun snoozeRinging(alarmId: Int) {
        viewModelScope.launch {
            AlarmSoundManager.stopRinging()
            AlarmStateHolder.setRingingAlarm(null)

            val notificationManager = getApplication<Application>().getSystemService(Application.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancel(alarmId)

            val alarm = repository.getAlarmById(alarmId)
            if (alarm != null) {
                val snoozeTime = Calendar.getInstance().apply {
                    add(Calendar.MINUTE, alarm.snoozeTimeMinutes)
                }.timeInMillis

                val alarmManager = getApplication<Application>().getSystemService(Application.ALARM_SERVICE) as? android.app.AlarmManager
                if (alarmManager != null) {
                    val snoozeIntent = android.content.Intent(getApplication(), com.example.receiver.AlarmReceiver::class.java).apply {
                        putExtra("ALARM_ID", alarm.id)
                    }
                    val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    } else {
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT
                    }
                    val pendingIntent = android.app.PendingIntent.getBroadcast(
                        getApplication(),
                        alarm.id,
                        snoozeIntent,
                        flags
                    )
                    try {
                        alarmManager.setExactAndAllowWhileIdle(
                            android.app.AlarmManager.RTC_WAKEUP,
                            snoozeTime,
                            pendingIntent
                        )
                    } catch (e: SecurityException) {
                        alarmManager.set(
                            android.app.AlarmManager.RTC_WAKEUP,
                            snoozeTime,
                            pendingIntent
                        )
                    }
                }
                increaseSnoozeStats()
            }
        }
    }

    fun testAlarmInFiveSeconds(puzzleType: String = "MATH", difficulty: String = "EASY") {
        viewModelScope.launch {
            val calendar = Calendar.getInstance().apply {
                add(Calendar.SECOND, 5)
            }
            val testAlarm = Alarm(
                id = 12345, // Fixed test ID
                hour = calendar.get(Calendar.HOUR_OF_DAY),
                minute = calendar.get(Calendar.MINUTE),
                isEnabled = true,
                label = "منبه تجريبي سريع 🚀",
                isVibrate = true,
                puzzleType = puzzleType,
                puzzleDifficulty = difficulty,
                snoozeTimeMinutes = 1
            )
            repository.insertAlarm(testAlarm)
            updateNextAlarmCountdown()
        }
    }

    private fun increaseWakeupStats() {
        _statsWakeups.value += 1
        _statsStreak.value += 1
    }

    private fun increaseSnoozeStats() {
        _statsSnoozes.value += 1
        _statsStreak.value = 0 // broke streak due to snooze!
    }
}
