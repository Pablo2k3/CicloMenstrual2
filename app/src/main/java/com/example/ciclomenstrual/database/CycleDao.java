package com.example.ciclomenstrual.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CycleDao {

    @Insert
    void insertCycle(RoomCycle cycle);

    @Update
    void updateCycle(RoomCycle cycle);

    //Método para actualizar la fecha de inicio de un ciclo sin terminar
    @Query("UPDATE cycles SET start_date = :newStartDate WHERE start_date = :oldStartDate AND end_date = 0")
    void updateStartDate(long oldStartDate, long newStartDate);

    //Método para actualizar la fecha de fin de un ciclo sin terminar
    @Query("UPDATE cycles SET end_date = :newEndDate WHERE start_date = :startDate AND end_date = 0")
    void updateEndDate(long startDate, long newEndDate);

    //Método para eliminar un ciclo por fecha de inicio
    @Query("DELETE FROM cycles WHERE start_date = :timeInMillis")
    void deleteCycleByStartDate(long timeInMillis);

    @Delete
    void deleteCycle(RoomCycle cycle);

    @Query("SELECT * FROM cycles order by start_date asc")
    List<RoomCycle> getAllCycles();
    @Query("SELECT * FROM cycles WHERE start_date = :timeInMillis")
    RoomCycle getCycleByStartDate(long timeInMillis);

    // Puedes agregar otros métodos aquí si es necesario, como:
    // @Query("SELECT * FROM cycles WHERE id = :id")
    // Cycle getCycleById(int id);

    // ...
}