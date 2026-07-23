package com.example.ciclomenstrual.domain

import com.example.ciclomenstrual.domain.model.ContraceptiveRegimen
import java.util.Calendar

class PillScheduleCalculator {
    fun pillNumber(regimen: ContraceptiveRegimen, date: Long): Int? {
        val day = DateNormalizer.normalize(date)
        val start = DateNormalizer.normalize(regimen.startDate)
        if (day < start || regimen.endDate?.let { day > DateNormalizer.normalize(it) } == true) return null
        val offset = calendarDaysBetween(start, day)
        return (offset % regimen.packLength).toInt() + 1
    }

    fun isPlacebo(regimen: ContraceptiveRegimen, pillNumber: Int): Boolean =
        pillNumber > regimen.activeDays

    fun doseTime(regimen: ContraceptiveRegimen, date: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = DateNormalizer.normalize(date)
            set(Calendar.HOUR_OF_DAY, regimen.doseHour)
            set(Calendar.MINUTE, regimen.doseMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun calendarDaysBetween(start: Long, end: Long): Long {
        var cursor = start
        var days = 0L
        while (cursor < end) {
            cursor = DateNormalizer.addDays(cursor, 1)
            days++
        }
        return days
    }
}
