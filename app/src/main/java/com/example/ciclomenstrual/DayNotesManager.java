package com.example.ciclomenstrual;

import com.example.ciclomenstrual.database.Note;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Centralises the handling of notes associated with specific calendar days.
 */
public class DayNotesManager {

    private final HashMap<Calendar, List<String>> dayNotes = new HashMap<>();

    public void loadFromEntities(List<Note> notes) {
        dayNotes.clear();
        if (notes == null) {
            return;
        }
        NoteConverter.loadNotesIntoMap(dayNotes, notes);
    }

    public List<String> getNotesForDate(Calendar date) {
        if (date == null) {
            return Collections.emptyList();
        }
        Calendar normalizedDate = normalize(date);
        List<String> notes = dayNotes.get(normalizedDate);
        if (notes == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(notes);
    }

    public void addNote(Calendar date, String content) {
        if (date == null || content == null) {
            return;
        }
        Calendar normalizedDate = normalize(date);
        dayNotes.computeIfAbsent(normalizedDate, key -> new ArrayList<>()).add(content);
    }

    public RemovedNote removeNote(Calendar date, int position) {
        if (date == null) {
            return null;
        }
        Calendar normalizedDate = normalize(date);
        List<String> notes = dayNotes.get(normalizedDate);
        if (notes == null || position < 0 || position >= notes.size()) {
            return null;
        }
        String removedContent = notes.remove(position);
        if (notes.isEmpty()) {
            dayNotes.remove(normalizedDate);
        }
        return new RemovedNote(normalizedDate, removedContent);
    }

    public boolean hasNotes(Calendar date) {
        if (date == null) {
            return false;
        }
        Calendar normalizedDate = normalize(date);
        List<String> notes = dayNotes.get(normalizedDate);
        return notes != null && !notes.isEmpty();
    }

    public boolean isEmpty(Calendar date) {
        if (date == null) {
            return true;
        }
        Calendar normalizedDate = normalize(date);
        List<String> notes = dayNotes.get(normalizedDate);
        return notes == null || notes.isEmpty();
    }

    public Collection<Calendar> getAllDatesWithNotes() {
        List<Calendar> dates = new ArrayList<>();
        for (Calendar calendar : dayNotes.keySet()) {
            dates.add((Calendar) calendar.clone());
        }
        return dates;
    }

    public Calendar normalize(Calendar date) {
        Calendar normalized = (Calendar) date.clone();
        normalized.set(Calendar.HOUR_OF_DAY, 0);
        normalized.set(Calendar.MINUTE, 0);
        normalized.set(Calendar.SECOND, 0);
        normalized.set(Calendar.MILLISECOND, 0);
        return normalized;
    }

    public static class RemovedNote {
        private final Calendar date;
        private final String content;

        RemovedNote(Calendar date, String content) {
            this.date = date;
            this.content = content;
        }

        public Calendar getDate() {
            return (Calendar) date.clone();
        }

        public String getContent() {
            return content;
        }
    }
}
