package com.example.ciclomenstrual.database;

import android.content.Context;

import com.example.ciclomenstrual.Cycle;
import com.example.ciclomenstrual.CycleConverter;
import com.example.ciclomenstrual.NoteConverter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CycleRepository {

    private final CycleDao cycleDao;
    private final NoteDao noteDao;
    private final ExecutorService executorService;

    public CycleRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        cycleDao = db.cycleDao();
        noteDao = db.noteDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public interface DataCallback<T> {
        void onResult(T result);
    }

    public void getAllCycles(DataCallback<List<Cycle>> callback) {
        executorService.execute(() -> {
            List<RoomCycle> roomCycles = cycleDao.getAllCycles();
            List<Cycle> cycles = new ArrayList<>();
            if (roomCycles != null) {
                for (RoomCycle roomCycle : roomCycles) {
                    cycles.add(CycleConverter.fromRoomCycle(roomCycle));
                }
            }
            callback.onResult(cycles);
        });
    }

    public void insertCycle(Cycle cycle, Runnable onComplete) {
        executorService.execute(() -> {
            cycleDao.insertCycle(CycleConverter.toRoomCycle(cycle));
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void updateCycle(Cycle cycle, Runnable onComplete) {
        executorService.execute(() -> {
            cycleDao.updateCycle(CycleConverter.toRoomCycle(cycle));
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void deleteCycle(Cycle cycle, Runnable onComplete) {
        executorService.execute(() -> {
            if (cycle.getStartDate() != null) {
                cycleDao.deleteCycleByStartDate(cycle.getStartDate().getTimeInMillis());
            }
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void getAllNotes(DataCallback<List<Note>> callback) {
        executorService.execute(() -> {
            List<Note> notes = noteDao.getAllNotes();
            callback.onResult(notes);
        });
    }

    public void insertNote(Note note, Runnable onComplete) {
        executorService.execute(() -> {
            noteDao.insert(note);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void deleteNote(long date, String content, Runnable onComplete) {
        executorService.execute(() -> {
            noteDao.deleteNoteByDateAndContent(date, content);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }
}
