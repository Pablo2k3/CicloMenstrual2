package com.example.ciclomenstrual.domain

import com.example.ciclomenstrual.domain.model.Cycle
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CycleRulesTest {
    private val madrid = TimeZone.getTimeZone(DateNormalizer.REFERENCE_TIME_ZONE_ID)
    private val today = date(2026, Calendar.JANUARY, 20)

    @Test fun `future start is rejected`() {
        assertEquals(
            CycleValidationError.START_IN_FUTURE,
            CycleRules.validateStart(DateNormalizer.addDays(today, 1), todayTimestamp()),
        )
    }

    @Test fun `valid end is accepted`() {
        assertNull(CycleRules.validateEnd(Cycle(DateNormalizer.addDays(today, -10)), today, todayTimestamp(), null))
    }

    @Test fun `end before start is rejected`() {
        assertEquals(
            CycleValidationError.END_BEFORE_START,
            CycleRules.validateEnd(
                Cycle(DateNormalizer.addDays(today, -5)),
                DateNormalizer.addDays(today, -6),
                todayTimestamp(),
                null,
            ),
        )
    }

    @Test fun `next cycle inside range is rejected`() {
        assertEquals(
            CycleValidationError.OVERLAPPING_CYCLE,
            CycleRules.validateEnd(
                Cycle(DateNormalizer.addDays(today, -10)),
                DateNormalizer.addDays(today, -2),
                todayTimestamp(),
                Cycle(DateNormalizer.addDays(today, -5)),
            ),
        )
    }

    @Test fun `only last incomplete cycle can be ongoing`() {
        val first = Cycle(DateNormalizer.addDays(today, -1))
        val last = Cycle(today)
        assertFalse(CycleRules.isOngoing(first, listOf(first, last), todayTimestamp()))
        assertTrue(CycleRules.isOngoing(last, listOf(first, last), todayTimestamp()))
    }

    private fun todayTimestamp(): Long = DateNormalizer.toLocalTimestamp(today, TimeZone.getDefault())

    private fun date(year: Int, month: Int, day: Int): Long = Calendar.getInstance(madrid).apply {
        clear()
        set(year, month, day, 0, 0, 0)
    }.timeInMillis.let { DateNormalizer.normalize(it, madrid) }
}
