package com.example.ciclomenstrual.domain

import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object DateNormalizer {
    const val REFERENCE_TIME_ZONE_ID = "Europe/Madrid"

    private val UTC = TimeZone.getTimeZone("UTC")

    /**
     * Converts an instant from the device into a stable, date-only key.
     * Date keys are stored at UTC midnight and must not be interpreted as local
     * midnights again.
     */
    fun normalize(timestamp: Long): Long = normalize(timestamp, TimeZone.getDefault())

    /** Converts an instant using an explicit calendar time zone. */
    fun normalize(timestamp: Long, timeZone: TimeZone): Long {
        val local = Calendar.getInstance(timeZone).apply { timeInMillis = timestamp }
        return dateKey(local)
    }

    /** Converts a legacy local-midnight timestamp into the stable date key. */
    fun legacyDateKey(timestamp: Long, timeZoneId: String = REFERENCE_TIME_ZONE_ID): Long {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(timeZoneId)).apply {
            timeInMillis = timestamp
        }
        return dateKey(calendar)
    }

    fun addDays(dateKey: Long, days: Int): Long = Calendar.getInstance(UTC).apply {
        timeInMillis = dateKey
        add(Calendar.DAY_OF_MONTH, days)
    }.timeInMillis

    fun daysBetween(startDateKey: Long, endDateKey: Long): Long =
        TimeUnit.MILLISECONDS.toDays(endDateKey - startDateKey)

    /** Returns the date key for the current local date at the supplied instant. */
    fun todayKey(now: Long = System.currentTimeMillis()): Long = normalize(now)

    /** Converts a stable date key into local midnight for UI/calendar APIs. */
    fun toLocalTimestamp(dateKey: Long, timeZone: TimeZone = TimeZone.getDefault()): Long {
        val fields = Calendar.getInstance(UTC).apply { timeInMillis = dateKey }
        return Calendar.getInstance(timeZone).apply {
            clear()
            set(
                fields.get(Calendar.YEAR),
                fields.get(Calendar.MONTH),
                fields.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0,
            )
        }.timeInMillis
    }

    /** Creates an instant at a local time for a stable date key. */
    fun atTime(
        dateKey: Long,
        timeZone: TimeZone,
        hour: Int,
        minute: Int,
    ): Long {
        val fields = Calendar.getInstance(UTC).apply { timeInMillis = dateKey }
        return Calendar.getInstance(timeZone).apply {
            clear()
            set(
                fields.get(Calendar.YEAR),
                fields.get(Calendar.MONTH),
                fields.get(Calendar.DAY_OF_MONTH),
                hour,
                minute,
                0,
            )
        }.timeInMillis
    }

    fun isDaylightSaving(dateKey: Long, timeZone: TimeZone): Boolean {
        val fields = Calendar.getInstance(UTC).apply { timeInMillis = dateKey }
        val midday = Calendar.getInstance(timeZone).apply {
            clear()
            set(
                fields.get(Calendar.YEAR),
                fields.get(Calendar.MONTH),
                fields.get(Calendar.DAY_OF_MONTH),
                12,
                0,
                0,
            )
        }
        return timeZone.inDaylightTime(midday.time)
    }

    private fun dateKey(calendar: Calendar): Long = Calendar.getInstance(UTC).apply {
        clear()
        set(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
            0,
            0,
            0,
        )
    }.timeInMillis
}
