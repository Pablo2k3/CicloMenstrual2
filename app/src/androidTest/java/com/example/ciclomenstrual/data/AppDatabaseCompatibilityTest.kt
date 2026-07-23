package com.example.ciclomenstrual.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ciclomenstrual.data.local.AppDatabase
import com.example.ciclomenstrual.data.local.RoomCycle
import com.example.ciclomenstrual.data.local.RoomNote
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseCompatibilityTest {
    @Test
    fun versionOneSchemaPersistsCyclesAndNotes() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            database.cycleDao().insert(RoomCycle(100, 200))
            database.noteDao().insert(RoomNote(date = 100, content = "nota"))

            assertEquals(RoomCycle(100, 200), database.cycleDao().getAll().single())
            assertEquals("nota", database.noteDao().getAll().single().content)
        } finally {
            database.close()
        }
    }
}
