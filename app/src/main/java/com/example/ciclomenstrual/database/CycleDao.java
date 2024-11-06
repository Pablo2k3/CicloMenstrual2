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
    void insertCycle(Cycle cycle);

    @Update
    void updateCycle(Cycle cycle);

    @Delete
    void deleteCycle(Cycle cycle);

    @Query("SELECT * FROM cycles order by start_date asc")
    List<Cycle> getAllCycles();
    @Query("SELECT * FROM cycles WHERE start_date = :timeInMillis")
    Cycle getCycleByStartDate(long timeInMillis);

    // Puedes agregar otros métodos aquí si es necesario, como:
    // @Query("SELECT * FROM cycles WHERE id = :id")
    // Cycle getCycleById(int id);

    // ...
}