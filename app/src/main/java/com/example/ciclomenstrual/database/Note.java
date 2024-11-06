package com.example.ciclomenstrual.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class Note {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "date")
    public long date; // Almacena la fecha como un long (milisegundos desde la época)

    @ColumnInfo(name = "text")
    public String text;

    // Constructor
    public Note(long date, String text) {
        this.date = date;
        this.text = text;
    }
}