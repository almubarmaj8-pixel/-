package com.example.receiver

import com.example.data.Alarm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AlarmStateHolder {
    private val _ringingAlarm = MutableStateFlow<Alarm?>(null)
    val ringingAlarm: StateFlow<Alarm?> = _ringingAlarm

    fun setRingingAlarm(alarm: Alarm?) {
        _ringingAlarm.value = alarm
    }
}
