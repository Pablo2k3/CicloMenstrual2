package com.example.ciclomenstrual.domain

import java.util.Calendar

object DateNormalizer {
    fun normalize(timestamp: Long): Long = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun addDays(timestamp: Long, days: Int): Long = Calendar.getInstance().apply {
        timeInMillis = timestamp
        add(Calendar.DAY_OF_MONTH, days)
    }.timeInMillis

    fun daysBetween(start: Long, end: Long): Long =
        java.util.concurrent.TimeUnit.MILLISECONDS.toDays(end - start)
}
