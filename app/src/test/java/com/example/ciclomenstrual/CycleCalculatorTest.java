package com.example.ciclomenstrual;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;

public class CycleCalculatorTest {

    @Test
    public void predictNextCycleStartAddsDefaultLength() {
        Calendar start = calendar(2024, Calendar.JANUARY, 1);
        Cycle cycle = new Cycle((Calendar) start.clone(), null);

        Calendar predicted = CycleCalculator.predictNextCycleStart(cycle, 28);

        assertEquals(calendar(2024, Calendar.JANUARY, 29).getTimeInMillis(), predicted.getTimeInMillis());
    }

    @Test
    public void ongoingCycleIsDetectedWithinThreshold() {
        Calendar reference = Calendar.getInstance();
        Calendar start = (Calendar) reference.clone();
        start.add(Calendar.DAY_OF_MONTH, -3);
        Cycle cycle = new Cycle(start, null);

        assertTrue(CycleCalculator.isOngoingCycle(cycle, 7, reference));
        assertFalse(CycleCalculator.isOngoingCycle(cycle, 2, reference));
    }

    @Test
    public void endDateBeforeStartIsDetected() {
        Calendar start = calendar(2024, Calendar.FEBRUARY, 10);
        Calendar end = calendar(2024, Calendar.FEBRUARY, 5);

        assertTrue(CycleCalculator.isEndDateBeforeStart(start, end));
        assertFalse(CycleCalculator.isEndDateBeforeStart(start, calendar(2024, Calendar.FEBRUARY, 12)));
    }

    @Test
    public void endOfDayReturnsLastMoment() {
        Calendar date = calendar(2024, Calendar.MARCH, 8);
        Calendar endOfDay = CycleCalculator.endOfDay(date);

        assertEquals(23, endOfDay.get(Calendar.HOUR_OF_DAY));
        assertEquals(59, endOfDay.get(Calendar.MINUTE));
        assertEquals(59, endOfDay.get(Calendar.SECOND));
        assertEquals(999, endOfDay.get(Calendar.MILLISECOND));
    }

    @Test
    public void isDateInFuture_returnsTrueForFutureDate() {
        Calendar now = Calendar.getInstance();
        Calendar future = (Calendar) now.clone();
        future.add(Calendar.DAY_OF_MONTH, 1);

        assertTrue(CycleCalculator.isDateInFuture(future, now));
        assertFalse(CycleCalculator.isDateInFuture(now, future));
    }

    @Test
    public void predictNextCycleStart_handlesNullCycle() {
        assertNull(CycleCalculator.predictNextCycleStart(null, 28));
        assertNull(CycleCalculator.predictNextCycleStart(new Cycle(null, null), 28));
    }

    private Calendar calendar(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(year, month, day, 0, 0, 0);
        return calendar;
    }
}
