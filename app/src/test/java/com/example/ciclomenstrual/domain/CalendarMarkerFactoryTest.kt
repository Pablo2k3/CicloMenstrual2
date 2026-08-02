package com.example.ciclomenstrual.domain

import com.example.ciclomenstrual.domain.model.Cycle
import com.example.ciclomenstrual.domain.model.MarkerBackground
import com.example.ciclomenstrual.domain.model.Note
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarMarkerFactoryTest {
    private val day = DateNormalizer.normalize(Calendar.getInstance().timeInMillis)

    @Test fun `complete cycle marks inclusive range`() {
        val end = DateNormalizer.addDays(day, 2)
        val result = CalendarMarkerFactory().create(listOf(Cycle(day, end)), emptyList(), day)
        assertEquals(3, result.first.count { it.background == MarkerBackground.PERIOD })
    }

    @Test fun `prediction remains 28 calendar days after start`() {
        val result = CalendarMarkerFactory().create(listOf(Cycle(day, day)), emptyList(), day)
        assertEquals(DateNormalizer.addDays(day, 28), result.second)
    }

    @Test fun `note and overdue prediction share one marker`() {
        val prediction = DateNormalizer.addDays(day, 28)
        val result = CalendarMarkerFactory().create(
            listOf(Cycle(day, day)),
            listOf(Note(date = prediction, content = "nota")),
            DateNormalizer.addDays(prediction, 1),
        )
        val marker = result.first.single { it.date == DateNormalizer.normalize(prediction) }
        assertTrue(marker.hasNote)
        assertTrue(marker.overduePrediction)
    }

    @Test fun `ongoing range ends at the current calendar day`() {
        val start = DateNormalizer.addDays(day, -1)
        val now = DateNormalizer.toLocalTimestamp(day, TimeZone.getDefault()) + 12 * 60 * 60 * 1_000

        val result = CalendarMarkerFactory().create(listOf(Cycle(start)), emptyList(), now)

        assertEquals(
            listOf(day),
            result.first.filter { it.background == MarkerBackground.ONGOING }.map { it.date },
        )
    }
}
