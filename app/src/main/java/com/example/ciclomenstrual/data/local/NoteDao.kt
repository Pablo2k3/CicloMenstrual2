package com.example.ciclomenstrual.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes")
    suspend fun getAll(): List<RoomNote>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: RoomNote)

    @Query("DELETE FROM notes WHERE date = :date AND content = :content")
    suspend fun deleteByDateAndContent(date: Long, content: String)
}
