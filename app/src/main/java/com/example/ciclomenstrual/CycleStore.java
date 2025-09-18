package com.example.ciclomenstrual;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * Maintains the in-memory representation of the recorded cycles and provides
 * helper operations to query or manipulate them.
 */
public class CycleStore {

    private final List<Cycle> cycles = new ArrayList<>();
    private Cycle selectedCycle;

    public void setInitialCycles(List<Cycle> initialCycles) {
        cycles.clear();
        if (initialCycles != null) {
            cycles.addAll(initialCycles);
        }
        selectedCycle = findLastIncompleteCycle();
    }

    public List<Cycle> getCycles() {
        return Collections.unmodifiableList(cycles);
    }

    public Cycle getSelectedCycle() {
        return selectedCycle;
    }

    public void setSelectedCycle(Cycle selectedCycle) {
        this.selectedCycle = selectedCycle;
    }

    public void clearSelection() {
        selectedCycle = null;
    }

    public void addCycleSorted(Cycle newCycle) {
        if (newCycle == null || newCycle.getStartDate() == null) {
            return;
        }
        int insertionIndex = 0;
        for (Cycle cycle : cycles) {
            if (newCycle.getStartDate().after(cycle.getStartDate())) {
                insertionIndex++;
            }
        }
        cycles.add(insertionIndex, newCycle);
        selectedCycle = newCycle;
    }

    public void removeCycle(Cycle cycle) {
        if (cycle == null) {
            return;
        }
        cycles.remove(cycle);
        if (cycle.equals(selectedCycle)) {
            selectedCycle = null;
        }
    }

    public Cycle findCycleForDate(Calendar date) {
        if (date == null) {
            return null;
        }
        for (Cycle cycle : cycles) {
            if (cycle.getStartDate() != null && cycle.getEndDate() != null && cycle.containsDate(date)) {
                return cycle;
            }
        }
        return null;
    }

    public Cycle getLastCycle() {
        if (cycles.isEmpty()) {
            return null;
        }
        return cycles.get(cycles.size() - 1);
    }

    public Cycle getLastCompleteCycle() {
        for (int i = cycles.size() - 1; i >= 0; i--) {
            Cycle cycle = cycles.get(i);
            if (cycle.getStartDate() != null && cycle.getEndDate() != null) {
                return cycle;
            }
        }
        return null;
    }

    public boolean isLastCycle(Cycle cycle) {
        return cycle != null && !cycles.isEmpty() && cycles.indexOf(cycle) == cycles.size() - 1;
    }

    public boolean hasOverlappingCycle(Cycle currentCycle, Calendar endDate) {
        if (currentCycle == null || endDate == null) {
            return false;
        }
        int index = cycles.indexOf(currentCycle);
        if (index >= 0 && index < cycles.size() - 1) {
            Cycle nextCycle = cycles.get(index + 1);
            return nextCycle.getStartDate() != null && nextCycle.getStartDate().before(endDate);
        }
        return false;
    }

    private Cycle findLastIncompleteCycle() {
        for (int i = cycles.size() - 1; i >= 0; i--) {
            Cycle cycle = cycles.get(i);
            if (cycle.getEndDate() == null) {
                return cycle;
            }
        }
        return null;
    }
}
