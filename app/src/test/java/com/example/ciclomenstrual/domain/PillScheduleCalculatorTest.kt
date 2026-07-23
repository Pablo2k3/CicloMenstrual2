package com.example.ciclomenstrual.domain

import com.example.ciclomenstrual.domain.model.ContraceptiveRegimen
import java.util.Calendar
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

    private fun date(year: Int, month: Int, day: Int) = Calendar.getInstance().apply {
        clear()
        set(year, month, day)
    }.timeInMillis
}
