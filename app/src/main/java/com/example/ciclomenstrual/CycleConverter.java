package com.example.ciclomenstrual;

import com.example.ciclomenstrual.database.Cycle; // Importar la entidad de Room


public class CycleConverter {

    public static com.example.ciclomenstrual.Cycle fromRoomCycle(Cycle roomCycle) {
        if (roomCycle == null) {
            return null;
        }

        com.example.ciclomenstrual.Cycle cycle = new com.example.ciclomenstrual.Cycle();
        if (roomCycle.getStartDate() != null) {
            cycle.setStartDate(roomCycle.getStartDate());
        }
        if (roomCycle.getEndDate() != null) {
            cycle.setEndDate(roomCycle.getEndDate());
        }
        // ... copiar otros campos si los tienes ...

        return cycle;
    }

    public static com.example.ciclomenstrual.database.Cycle toRoomCycle(com.example.ciclomenstrual.Cycle cycle) {
        if (cycle == null) {
            return null;
        }

        com.example.ciclomenstrual.database.Cycle roomCycle = new com.example.ciclomenstrual.database.Cycle();
        if (cycle.getStartDate() != null) {
            roomCycle.setStartDate(cycle.getStartDate().getTimeInMillis());
        }
        if (cycle.getEndDate() != null) {
            roomCycle.setEndDate(cycle.getEndDate().getTimeInMillis());
        }
        // ... copiar otros campos si los tienes ...

        return roomCycle;
    }
}