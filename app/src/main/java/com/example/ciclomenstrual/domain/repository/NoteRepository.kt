package com.example.ciclomenstrual.domain.repository

import com.example.ciclomenstrual.domain.model.Note

interface NoteRepository {
    suspend fun getAll(): List<Note>
    suspend fun insert(note: Note): Long
    suspend fun deleteById(id: Long)
}
