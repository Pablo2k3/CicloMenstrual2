package com.example.ciclomenstrual.domain

import com.example.ciclomenstrual.domain.model.ContraceptiveRegimen
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PillScheduleCalculatorTest {
    private val calculator = PillScheduleCalculator()
    private val start = date(2026, Calendar.JANUARY, 20)
    private val regimen = ContraceptiveRegimen(id = 1, startDate = start)

    @Test fun `numbers cycle from one to twenty eight`() {
        assertEquals(1, calculator.pillNumber(regimen, start))
        assertEquals(21, calculator.pillNumber(regimen, DateNormalizer.addDays(start, 20)))
        assertEquals(22, calculator.pillNumber(regimen, DateNormalizer.addDays(start, 21)))
        assertEquals(28, calculator.pillNumber(regimen, DateNormalizer.addDays(start, 27)))
        assertEquals(1, calculator.pillNumber(regimen, DateNormalizer.addDays(start, 28)))
    }

    @Test fun `only pills twenty two to twenty eight are placebo`() {
        assertFalse(calculator.isPlacebo(regimen, 21))
        assertTrue(calculator.isPlacebo(regimen, 22))
        assertTrue(calculator.isPlacebo(regimen, 28))
    }

    @Test fun `calendar arithmetic crosses month and year`() {
        val december = ContraceptiveRegimen(startDate = date(2025, Calendar.DECEMBER, 31))
        assertEquals(2, calculator.pillNumber(december, date(2026, Calendar.JANUARY, 1)))
    }

    private fun date(year: Int, month: Int, day: Int): Long = Calendar.getInstance(MADRID).apply {
        clear()
        set(year, month, day, 0, 0, 0)
    }.timeInMillis.let { DateNormalizer.normalize(it, MADRID) }

    @Test
    fun `dose is fourteen in Madrid during summer and eleven three hours behind`() {
        val summerDay = date(2026, Calendar.JULY, 23)
        val dose = calculator.doseTime(regimen.copy(startDate = summerDay), summerDay)

        assertEquals(14, hourIn(dose, MADRID))
        assertEquals(11, hourIn(dose, TimeZone.getTimeZone("GMT-01:00")))
    }

    @Test
    fun `dose is thirteen in Madrid during winter and ten three hours behind`() {
        val winterDay = date(2026, Calendar.JANUARY, 23)
        val dose = calculator.doseTime(regimen.copy(startDate = winterDay), winterDay)

        assertEquals(13, hourIn(dose, MADRID))
        assertEquals(10, hourIn(dose, TimeZone.getTimeZone("GMT-02:00")))
    }

    @Test
    fun `Spain daylight saving transition keeps consecutive doses twenty four hours apart`() {
        val beforeTransition = date(2026, Calendar.OCTOBER, 24)
        val afterTransition = date(2026, Calendar.OCTOBER, 25)
        val previousDose = calculator.doseTime(regimen, beforeTransition)
        val nextDose = calculator.doseTime(regimen, afterTransition)

        assertEquals(TimeUnit.DAYS.toMillis(1), nextDose - previousDose)
    }

    private fun hourIn(timestamp: Long, timeZone: TimeZone): Int =
        Calendar.getInstance(timeZone).apply { timeInMillis = timestamp }.get(Calendar.HOUR_OF_DAY)

    private companion object {
        val MADRID: TimeZone = TimeZone.getTimeZone(DateNormalizer.REFERENCE_TIME_ZONE_ID)
    }
}
