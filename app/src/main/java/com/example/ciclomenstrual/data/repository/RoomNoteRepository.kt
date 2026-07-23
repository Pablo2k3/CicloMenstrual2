package com.example.ciclomenstrual.data.repository

import com.example.ciclomenstrual.data.local.NoteDao
import com.example.ciclomenstrual.data.local.RoomNote
import com.example.ciclomenstrual.domain.model.Note
import com.example.ciclomenstrual.domain.repository.NoteRepository

class RoomNoteRepository(private val dao: NoteDao) : NoteRepository {
    override suspend fun getAll(): List<Note> =
        dao.getAll().map { Note(it.id, it.date, it.content.orEmpty()) }

    override suspend fun insert(note: Note) =
        dao.insert(RoomNote(note.id, note.date, note.content))

    override suspend fun deleteByDateAndContent(date: Long, content: String) =
        dao.deleteByDateAndContent(date, content)
}
