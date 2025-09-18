package com.example.ciclomenstrual;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

public class DayNotesManagerTest {

    private DayNotesManager manager;

    @Before
    public void setUp() {
        manager = new DayNotesManager();
    }

    @Test
    public void normalizeRemovesTimeComponents() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2024, Calendar.JUNE, 15, 10, 30, 45);
        calendar.set(Calendar.MILLISECOND, 123);

        Calendar normalized = manager.normalize(calendar);

        assertEquals(0, normalized.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, normalized.get(Calendar.MINUTE));
        assertEquals(0, normalized.get(Calendar.SECOND));
        assertEquals(0, normalized.get(Calendar.MILLISECOND));
    }

    @Test
    public void addAndRemoveNotesKeepsManagerStateConsistent() {
        Calendar date = Calendar.getInstance();
        date.set(2024, Calendar.MARCH, 3, 12, 0, 0);

        manager.addNote(date, "note");

        List<String> notes = manager.getNotesForDate(date);
        assertEquals(1, notes.size());
        assertEquals("note", notes.get(0));

        DayNotesManager.RemovedNote removedNote = manager.removeNote(date, 0);
        assertNotNull(removedNote);
        assertEquals("note", removedNote.getContent());
        assertTrue(manager.getNotesForDate(date).isEmpty());
    }

    @Test
    public void datesWithNotesAreReturnedAsCopies() {
        Calendar date = Calendar.getInstance();
        date.set(2024, Calendar.APRIL, 10, 8, 15, 0);

        manager.addNote(date, "entry");

        Iterator<Calendar> iterator = manager.getAllDatesWithNotes().iterator();
        assertTrue(iterator.hasNext());
        Calendar returnedDate = iterator.next();
        returnedDate.add(Calendar.DAY_OF_MONTH, 1);

        assertTrue(manager.hasNotes(date));
    }
}
