package com.example.ciclomenstrual.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CycleDao {
    @Insert
    suspend fun insert(cycle: RoomCycle)

    @Query("UPDATE cycles SET end_date = :endDate WHERE start_date = :startDate AND end_date = 0")
    suspend fun updateEnd(startDate: Long, endDate: Long)

    @Delete
    suspend fun delete(cycle: RoomCycle)

    @Query("DELETE FROM cycles WHERE start_date = :startDate")
    suspend fun deleteByStartDate(startDate: Long)

    @Query("SELECT * FROM cycles ORDER BY start_date ASC")
    suspend fun getAll(): List<RoomCycle>

    @Query("SELECT EXISTS(SELECT 1 FROM cycles WHERE start_date = :startDate)")
    suspend fun exists(startDate: Long): Boolean
}
