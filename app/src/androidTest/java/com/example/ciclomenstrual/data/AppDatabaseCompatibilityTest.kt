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
import android.database.sqlite.SQLiteDatabase

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

    @Test
    fun migratesRealVersionOneShapeWithoutLosingCyclesOrNotes() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-v1-v2-test"
        context.deleteDatabase(name)
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { legacy ->
            legacy.execSQL("CREATE TABLE cycles (start_date INTEGER NOT NULL PRIMARY KEY, end_date INTEGER NOT NULL)")
            legacy.execSQL(
                "CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date INTEGER NOT NULL, content TEXT)",
            )
            legacy.execSQL("INSERT INTO cycles VALUES (100, 200)")
            legacy.execSQL("INSERT INTO notes(date, content) VALUES (100, 'legacy')")
            legacy.version = 1
        }
        val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
        try {
            assertEquals(1, database.cycleDao().getAll().size)
            assertEquals("legacy", database.noteDao().getAll().single().content)
            assertEquals(0, database.contraceptiveDao().getRegimens().size)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
