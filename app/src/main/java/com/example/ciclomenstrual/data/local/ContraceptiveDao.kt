package com.example.ciclomenstrual.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ContraceptiveDao {
    @Query("SELECT * FROM contraceptive_regimens ORDER BY start_date ASC")
    suspend fun getRegimens(): List<RoomContraceptiveRegimen>

    @Query("SELECT * FROM contraceptive_regimens WHERE end_date IS NULL ORDER BY start_date DESC LIMIT 1")
    suspend fun getActiveRegimen(): RoomContraceptiveRegimen?

    @Query("SELECT * FROM pill_intakes ORDER BY scheduled_date ASC")
    suspend fun getIntakes(): List<RoomPillIntake>

    @Insert
    suspend fun insertRegimen(regimen: RoomContraceptiveRegimen): Long

    @Query("UPDATE contraceptive_regimens SET end_date = :endDate WHERE end_date IS NULL")
    suspend fun closeActiveRegimen(endDate: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntakes(intakes: List<RoomPillIntake>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntake(intake: RoomPillIntake)

    @Query("DELETE FROM pill_intakes WHERE regimen_id = :regimenId AND scheduled_date = :date")
    suspend fun deleteIntake(regimenId: Long, date: Long)
}
