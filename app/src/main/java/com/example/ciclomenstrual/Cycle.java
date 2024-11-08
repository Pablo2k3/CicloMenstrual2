package com.example.ciclomenstrual;

import java.util.Calendar;

public class Cycle {
    private Calendar startDate;
    private Calendar endDate;

    public Cycle(Calendar startDate, Calendar endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Cycle() {

    }

    public Calendar getStartDate() {
        return startDate;
    }

    public void setStartDate(Calendar startDate) {
        this.startDate = startDate;
    }

    public Calendar getEndDate() {
        return endDate;
    }

    public void setEndDate(Calendar endDate) {
        this.endDate = endDate;
    }

    public boolean containsDate(Calendar date) {
        return !date.before(startDate) && !date.after(endDate);
    }

    //método que me permita ordenar los ciclos por fecha de inicio si en la lista hago un .sort()
    public int compareTo(Cycle cycle) {
        return startDate.compareTo(cycle.getStartDate());
    }
}