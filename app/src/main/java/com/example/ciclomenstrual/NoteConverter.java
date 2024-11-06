package com.example.ciclomenstrual;

import com.example.ciclomenstrual.database.Note;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class NoteConverter {
    public static Calendar dateFromTimestamp(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        return calendar;
    }

    public static long dateToTimestamp(Calendar calendar) {
        return calendar.getTimeInMillis();
    }

    public static void loadNotesIntoMap(HashMap<Calendar, List<String>> dayNotes, List<Note> notes) {
        dayNotes.clear();
        for (Note note : notes) {
            Calendar calendar = dateFromTimestamp(note.getDate());
            // Normalizar la fecha (eliminar hora, minutos, etc.)
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            if (!dayNotes.containsKey(calendar)) {
                dayNotes.put(calendar, new ArrayList<>());
            }
            dayNotes.get(calendar).add(note.getContent());
        }
    }
}