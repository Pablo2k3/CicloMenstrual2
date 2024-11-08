package com.example.ciclomenstrual.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Calendar;
import java.util.GregorianCalendar;

@Entity(tableName = "cycles")
public class RoomCycle {
    @PrimaryKey
    @ColumnInfo(name = "start_date")
    public long startDate;

    @ColumnInfo(name = "end_date")
    public long endDate;

    // ... otros campos ...

    // Métodos getStartDate() y getEndDate()
    public Calendar getStartDate() {
        if (startDate == 0) {
            return null;
        }
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(startDate);
        return calendar;
    }

    public Calendar getEndDate() {
        if (endDate == 0) {
            return null;
        }
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(endDate);
        return calendar;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }
    public com.example.ciclomenstrual.Cycle toOriginalCycle() {
        com.example.ciclomenstrual.Cycle cycle = new com.example.ciclomenstrual.Cycle();
        cycle.setStartDate(this.getStartDate());
        cycle.setEndDate(this.getEndDate());
        // ... copiar otros campos ...
        return cycle;
    }

    public boolean containsDate(Calendar date) {
        Calendar startDate = getStartDate();
        Calendar endDate = getEndDate();

        if (startDate != null && endDate != null) {
            return !date.before(startDate) && !date.after(endDate);
        } else if (startDate != null) {
            return !date.before(startDate);
        } else if (endDate != null) {
            return !date.after(endDate);
        } else {
            return false;
        }
    }

}