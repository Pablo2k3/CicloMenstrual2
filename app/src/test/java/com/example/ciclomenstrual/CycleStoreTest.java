package com.example.ciclomenstrual;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CycleStoreTest {

    @Test
    public void initialSelectionTargetsIncompleteCycle() {
        CycleStore store = new CycleStore();

        List<Cycle> cycles = new ArrayList<>();
        cycles.add(new Cycle(calendar(2024, Calendar.JANUARY, 1), calendar(2024, Calendar.JANUARY, 5)));
        cycles.add(new Cycle(calendar(2024, Calendar.JANUARY, 10), null));

        store.setInitialCycles(cycles);

        Cycle selected = store.getSelectedCycle();
        assertNotNull(selected);
        assertEquals(calendar(2024, Calendar.JANUARY, 10).getTimeInMillis(),
                selected.getStartDate().getTimeInMillis());
        assertEquals(calendar(2024, Calendar.JANUARY, 1).getTimeInMillis(),
                store.getLastCompleteCycle().getStartDate().getTimeInMillis());
    }

    @Test
    public void addCycleSortedMaintainsOrder() {
        CycleStore store = new CycleStore();

        Cycle later = new Cycle(calendar(2024, Calendar.JANUARY, 15), null);
        store.addCycleSorted(later);

        Cycle earlier = new Cycle(calendar(2024, Calendar.JANUARY, 5), null);
        store.addCycleSorted(earlier);

        List<Cycle> stored = new ArrayList<>(store.getCycles());
        assertEquals(earlier.getStartDate().getTimeInMillis(), stored.get(0).getStartDate().getTimeInMillis());
        assertEquals(later.getStartDate().getTimeInMillis(), stored.get(1).getStartDate().getTimeInMillis());
    }

    @Test
    public void overlappingCyclesAreDetected() {
        CycleStore store = new CycleStore();

        Cycle first = new Cycle(calendar(2024, Calendar.JANUARY, 1), null);
        store.addCycleSorted(first);
        Cycle second = new Cycle(calendar(2024, Calendar.JANUARY, 10), null);
        store.addCycleSorted(second);

        Calendar endDate = calendar(2024, Calendar.JANUARY, 12);
        assertTrue(store.hasOverlappingCycle(first, endDate));
    }

    private Calendar calendar(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(year, month, day, 0, 0, 0);
        return calendar;
    }
}
