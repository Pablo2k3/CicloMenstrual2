package com.example.ciclomenstrual.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.ciclomenstrual.database.Note;

import java.util.List;

@Dao
public interface NoteDao {
    @Query("SELECT * FROM notes WHERE date = :date")
    List<Note> getNotesForDate(long date);

    @Query("SELECT * FROM notes")
    List<Note> getAllNotes();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Note note);

    @Delete
    void delete(Note note);

    @Query("DELETE FROM notes WHERE date = :date AND content = :content")
    void deleteNoteByDateAndContent(long date, String content);
}