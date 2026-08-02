package com.example.ciclomenstrual.domain

import com.example.ciclomenstrual.domain.model.ContraceptiveRegimen
import java.util.TimeZone

class PillScheduleCalculator {
    fun pillNumber(regimen: ContraceptiveRegimen, date: Long): Int? {
        val day = date
        val start = regimen.startDate
        if (day < start || regimen.endDate?.let { day > it } == true) return null
        val offset = calendarDaysBetween(start, day)
        return (offset % regimen.packLength).toInt() + 1
    }

    fun isPlacebo(regimen: ContraceptiveRegimen, pillNumber: Int): Boolean =
        pillNumber > regimen.activeDays

    fun doseTime(regimen: ContraceptiveRegimen, date: Long): Long {
        val referenceZone = TimeZone.getTimeZone(DateNormalizer.REFERENCE_TIME_ZONE_ID)
        val hour = if (DateNormalizer.isDaylightSaving(date, referenceZone)) 14 else 13
        return DateNormalizer.atTime(date, referenceZone, hour, 0)
    }

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
