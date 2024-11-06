package com.example.ciclomenstrual.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Cycle.class, Note.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract CycleDao cycleDao();
    public abstract NoteDao noteDao();

    private static AppDatabase instance;

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "app_database")
                    .build();
        }
        return instance;
    }
}