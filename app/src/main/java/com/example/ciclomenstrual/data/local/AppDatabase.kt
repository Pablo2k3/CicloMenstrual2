package com.example.ciclomenstrual.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        RoomCycle::class,
        RoomNote::class,
        RoomContraceptiveRegimen::class,
        RoomPillIntake::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cycleDao(): CycleDao
    abstract fun noteDao(): NoteDao
    abstract fun contraceptiveDao(): ContraceptiveDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database",
                ).addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `contraceptive_regimens` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `start_date` INTEGER NOT NULL,
                        `end_date` INTEGER,
                        `dose_hour` INTEGER NOT NULL,
                        `dose_minute` INTEGER NOT NULL,
                        `active_days` INTEGER NOT NULL,
                        `placebo_days` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pill_intakes` (
                        `regimen_id` INTEGER NOT NULL,
                        `scheduled_date` INTEGER NOT NULL,
                        `pill_number` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `taken_at` INTEGER,
                        `source` TEXT NOT NULL,
                        PRIMARY KEY(`regimen_id`, `scheduled_date`),
                        FOREIGN KEY(`regimen_id`) REFERENCES `contraceptive_regimens`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_pill_intakes_regimen_id` ON `pill_intakes` (`regimen_id`)",
                )
            }
        }
    }
}
