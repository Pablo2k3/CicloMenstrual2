package com.example.ciclomenstrual.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class Note {
    @PrimaryKey(autoGenerate = true)
    private long id;

    private long date; // Almacenaremos la fecha como timestamp
    private String content;

    public Note(long date, String content) {
        this.date = date;
        this.content = content;
    }

    // Getters y setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}