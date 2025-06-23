package com.example.adaptivesecurity.context.collectors

import com.example.adaptivesecurity.context.models.TimeContext
import com.example.adaptivesecurity.context.models.TimeOfDay
import java.util.*

class TimeContextCollector {

    fun collectTimeContext(): TimeContext {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        return TimeContext(
            timeOfDay = determineTimeOfDay(hour),
            isWorkingHours = isWorkingHours(hour)
        )
    }

    private fun determineTimeOfDay(hour: Int): TimeOfDay {
        return when (hour) {
            in 0..5 -> TimeOfDay.NIGHT
            in 6..11 -> TimeOfDay.MORNING
            in 12..16 -> TimeOfDay.AFTERNOON
            in 17..21 -> TimeOfDay.EVENING
            else -> TimeOfDay.NIGHT
        }
    }

    private fun isWorkingHours(hour: Int): Boolean {
        return hour in 9..16 // 9 AM to 5 PM
    }
}
