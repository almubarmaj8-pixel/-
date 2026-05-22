package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int, // 0-23
    val minute: Int, // 0-59
    val isEnabled: Boolean = true,
    val label: String = "",
    val daysToRepeat: String = "", // Comma-separated indices, e.g., "0,1,2" (0=Sat, 1=Sun, 2=Mon, 3=Tue, 4=Wed, 5=Thu, 6=Fri)
    val isVibrate: Boolean = true,
    val isGentleWakeUp: Boolean = true, // Gradual volume increase
    val puzzleType: String = "NONE", // "NONE", "MATH", "SHAKE"
    val puzzleDifficulty: String = "MEDIUM", // "EASY", "MEDIUM", "HARD"
    val snoozeTimeMinutes: Int = 5, // Snooze duration in minutes
    val isSnoozed: Boolean = false,
    val soundUri: String = "" // Custom tone URI or empty for default
) {
    fun getFormattedTime(): String {
        val h = if (hour == 0 || hour == 12) 12 else hour % 12
        val amPm = if (hour < 12) "ص" else "م"
        return String.format("%02d:%02d %s", h, minute, amPm)
    }

    fun getRepeatDaysFormatted(): String {
        if (daysToRepeat.isEmpty()) return "مرة واحدة"
        val days = daysToRepeat.split(",")
            .filter { it.isNotEmpty() }
            .mapNotNull { it.toIntOrNull() }
            .sorted()
        
        if (days.size == 7) return "يومياً"
        
        // standard Middle East work week (Sun-Thu) and weekend (Fri-Sat)
        val workdays = listOf(1, 2, 3, 4, 5) // Sun, Mon, Tue, Wed, Thu
        val weekend = listOf(0, 6) // Sat, Fri
        
        if (days == workdays) return "أيام العمل (الأحد - الخميس)"
        if (days == weekend) return "عطلة نهاية الأسبوع (الجمعة - السبت)"
        
        val dayNames = listOf("السبت", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة")
        return days.mapNotNull { dayNames.getOrNull(it) }.joinToString("، ")
    }

    // Calculates next trigger time in milliseconds from now
    fun getNextTriggerMillis(): Long {
        val calendar = Calendar.getInstance()
        val now = calendar.clone() as Calendar
        
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        if (daysToRepeat.isEmpty()) {
            // Alarm fires once
            if (calendar.before(now)) {
                // It was earlier today, so set to tomorrow
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            return calendar.timeInMillis
        } else {
            // Alarm repeats on specified days
            val days = daysToRepeat.split(",")
                .filter { it.isNotEmpty() }
                .mapNotNull { it.toIntOrNull() }
            
            // Map our custom indices to Calendar.DAY_OF_WEEK
            // Custom: 0=Sat, 1=Sun, 2=Mon, 3=Tue, 4=Wed, 5=Thu, 6=Fri
            // Calendar: Calendar.SATURDAY (7), Calendar.SUNDAY (1), Calendar.MONDAY (2), Calendar.TUESDAY (3), Calendar.WEDNESDAY (4), Calendar.THURSDAY (5), Calendar.FRIDAY (6)
            val customToCalendarDay = mapOf(
                0 to Calendar.SATURDAY,
                1 to Calendar.SUNDAY,
                2 to Calendar.MONDAY,
                3 to Calendar.TUESDAY,
                4 to Calendar.WEDNESDAY,
                5 to Calendar.THURSDAY,
                6 to Calendar.FRIDAY
            )

            val targetCalendarDays = days.mapNotNull { customToCalendarDay[it] }
            
            // Start checking from today onwards to find the closest repeating day
            var minDiffDays = 8
            var bestTriggerTime: Long = 0
            
            for (targetDay in targetCalendarDays) {
                val tempCal = calendar.clone() as Calendar
                val todayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK)
                
                var diff = targetDay - todayOfWeek
                if (diff < 0) {
                    diff += 7
                } else if (diff == 0) {
                    if (tempCal.before(now)) {
                        diff = 7 // Scheduled for next week
                    }
                }
                
                tempCal.add(Calendar.DAY_OF_YEAR, diff)
                if (bestTriggerTime == 0L || tempCal.timeInMillis < bestTriggerTime) {
                    bestTriggerTime = tempCal.timeInMillis
                }
            }
            return if (bestTriggerTime != 0L) bestTriggerTime else calendar.timeInMillis
        }
    }
}
