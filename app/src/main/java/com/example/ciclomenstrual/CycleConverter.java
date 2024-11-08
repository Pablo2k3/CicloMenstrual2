package com.example.ciclomenstrual;


import com.example.ciclomenstrual.database.RoomCycle;

import java.util.ArrayList;
import java.util.List;

public class CycleConverter {

    public static Cycle fromRoomCycle(RoomCycle roomCycle) {
        if (roomCycle == null) {
            return null;
        }

        Cycle cycle = new Cycle();
        if (roomCycle.getStartDate() != null) {
            cycle.setStartDate(roomCycle.getStartDate());
        }
        if (roomCycle.getEndDate() != null) {
            cycle.setEndDate(roomCycle.getEndDate());
        }
        // ... copiar otros campos si los tienes ...

        return cycle;
    }

    public static RoomCycle toRoomCycle(Cycle cycle) {
        if (cycle == null) {
            return null;
        }

        RoomCycle roomCycle = new RoomCycle();
        if (cycle.getStartDate() != null) {
            roomCycle.setStartDate(cycle.getStartDate().getTimeInMillis());
        }
        if (cycle.getEndDate() != null) {
            roomCycle.setEndDate(cycle.getEndDate().getTimeInMillis());
        }
        return roomCycle;
    }

    // creamos los métodos para convertir lista de roomCycle a lista de Cycle y al revés
    public static List<Cycle> fromRoomCycles(List<RoomCycle> roomCycles) {
        List<Cycle> cycles = new ArrayList<>();
        for (RoomCycle roomCycle : roomCycles) {
            cycles.add(fromRoomCycle(roomCycle));
        }
        return cycles;
    }
    // y al contrario
    public static List<RoomCycle> toRoomCycles(List<Cycle> cycles) {
        List<RoomCycle> roomCycles = new ArrayList<>();
        for (Cycle cycle : cycles) {
            roomCycles.add(toRoomCycle(cycle));
        }
        return roomCycles;
    }


}