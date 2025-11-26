package com.example.ciclomenstrual;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Provides reusable calculations for cycle related operations.
 */
public final class CycleCalculator {

    private CycleCalculator() {
        // Utility class
    }

    public static boolean isDateInFuture(Calendar date, Calendar reference) {
        return date != null && reference != null && date.after(reference);
    }

    public static boolean isEndDateBeforeStart(Calendar startDate, Calendar endDate) {
        return startDate != null && endDate != null && endDate.before(startDate);
    }

    public static Calendar predictNextCycleStart(Cycle cycle, int defaultLength) {
        if (cycle == null || cycle.getStartDate() == null) {
            return null;
        }
        Calendar nextStart = (Calendar) cycle.getStartDate().clone();
        nextStart.add(Calendar.DAY_OF_MONTH, defaultLength);
        return nextStart;
    }

    public static boolean isOngoingCycle(Cycle cycle, int maxOngoingDays, Calendar referenceDate) {
        if (cycle == null || cycle.getStartDate() == null || referenceDate == null) {
            return false;
        }
        if (cycle.getEndDate() != null) {
            return false;
        }
        long daysSinceStart = TimeUnit.MILLISECONDS.toDays(
                referenceDate.getTimeInMillis() - cycle.getStartDate().getTimeInMillis());
        return daysSinceStart < maxOngoingDays;
    }

    public static Calendar endOfDay(Calendar date) {
        if (date == null) {
            return null;
        }
        Calendar endOfDay = (Calendar) date.clone();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);
        return endOfDay;
    }
}
