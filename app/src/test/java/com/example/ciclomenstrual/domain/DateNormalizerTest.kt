package com.example.ciclomenstrual.domain

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class DateNormalizerTest {
    private val madrid = TimeZone.getTimeZone(DateNormalizer.REFERENCE_TIME_ZONE_ID)
    private val deviceTimeZone = TimeZone.getTimeZone("GMT-02:00")

    @Test
    fun `same calendar date has one stable key in every device zone`() {
        val madridMidnight = Calendar.getInstance(madrid).apply {
            clear()
            set(2026, Calendar.JULY, 23, 0, 0, 0)
        }.timeInMillis
        val deviceInstant = madridMidnight + 3 * 60 * 60 * 1_000

        assertEquals(
            DateNormalizer.normalize(madridMidnight, madrid),
            DateNormalizer.normalize(deviceInstant, madrid),
        )
        assertEquals(
            23,
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = DateNormalizer.normalize(deviceInstant, madrid)
            }.get(Calendar.DAY_OF_MONTH),
        )
    }

    @Test
    fun `stable date key converts to local calendar midnight`() {
        val dateKey = DateNormalizer.normalize(
            Calendar.getInstance(madrid).apply {
                clear()
                set(2026, Calendar.JANUARY, 23, 0, 0, 0)
            }.timeInMillis,
            madrid,
        )

        val local = Calendar.getInstance(deviceTimeZone).apply {
            timeInMillis = DateNormalizer.toLocalTimestamp(dateKey, deviceTimeZone)
        }
        assertEquals(0, local.get(Calendar.HOUR_OF_DAY))
        assertEquals(23, local.get(Calendar.DAY_OF_MONTH))
    }
}
