package com.example.ciclomenstrual.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ciclomenstrual.data.local.AppDatabase
import com.example.ciclomenstrual.data.local.RoomCycle
import com.example.ciclomenstrual.data.local.RoomNote
import com.example.ciclomenstrual.domain.DateNormalizer
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import android.database.sqlite.SQLiteDatabase

@RunWith(AndroidJUnit4::class)
class AppDatabaseCompatibilityTest {
    @Test
    fun currentSchemaPersistsCyclesAndNotes() = runBlocking {
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
    fun deletingOneDuplicateNoteKeepsTheOther() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val firstId = database.noteDao().insert(RoomNote(date = 100, content = "nota"))
            val secondId = database.noteDao().insert(RoomNote(date = 100, content = "nota"))

            database.noteDao().deleteById(firstId)

            assertEquals(listOf(secondId), database.noteDao().getAll().map { it.id })
        } finally {
            database.close()
        }
    }

    @Test
    fun migratesRealVersionOneShapeWithoutLosingCyclesOrNotes() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-v1-v3-test"
        context.deleteDatabase(name)
        val legacyStart = legacyMadridDate(2026, Calendar.JANUARY, 10)
        val legacyEnd = legacyMadridDate(2026, Calendar.JANUARY, 12)
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { legacy ->
            legacy.execSQL("CREATE TABLE cycles (start_date INTEGER NOT NULL PRIMARY KEY, end_date INTEGER NOT NULL)")
            legacy.execSQL(
                "CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date INTEGER NOT NULL, content TEXT)",
            )
            legacy.execSQL("INSERT INTO cycles VALUES ($legacyStart, $legacyEnd)")
            legacy.execSQL("INSERT INTO notes(date, content) VALUES ($legacyStart, 'legacy')")
            legacy.version = 1
        }
        val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()
        try {
            val cycle = database.cycleDao().getAll().single()
            assertEquals(DateNormalizer.legacyDateKey(legacyStart), cycle.startDate)
            assertEquals(DateNormalizer.legacyDateKey(legacyEnd), cycle.endDate)
            val note = database.noteDao().getAll().single()
            assertEquals(DateNormalizer.legacyDateKey(legacyStart), note.date)
            assertEquals("legacy", note.content)
            assertEquals(0, database.contraceptiveDao().getRegimens().size)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    private fun legacyMadridDate(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone(DateNormalizer.REFERENCE_TIME_ZONE_ID)).apply {
            clear()
            set(year, month, day, 0, 0, 0)
        }.timeInMillis
}
