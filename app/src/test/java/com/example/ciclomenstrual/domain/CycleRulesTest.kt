package com.example.ciclomenstrual.domain

import com.example.ciclomenstrual.domain.model.Cycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CycleRulesTest {
    @Test fun `future start is rejected`() {
        assertEquals(CycleValidationError.START_IN_FUTURE, CycleRules.validateStart(11, 10))
    }

    @Test fun `valid end is accepted`() {
        assertNull(CycleRules.validateEnd(Cycle(10), 12, 20, null))
    }

    @Test fun `end before start is rejected`() {
        assertEquals(
            CycleValidationError.END_BEFORE_START,
            CycleRules.validateEnd(Cycle(10), 9, 20, null),
        )
    }

    @Test fun `next cycle inside range is rejected`() {
        assertEquals(
            CycleValidationError.OVERLAPPING_CYCLE,
            CycleRules.validateEnd(Cycle(10), 20, 30, Cycle(15)),
        )
    }

    @Test fun `only last incomplete cycle can be ongoing`() {
        val first = Cycle(0)
        val last = Cycle(1)
        assertFalse(CycleRules.isOngoing(first, listOf(first, last), 2))
        assertTrue(CycleRules.isOngoing(last, listOf(first, last), 2))
    }
}
