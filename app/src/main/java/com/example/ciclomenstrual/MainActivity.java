package com.example.ciclomenstrual;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.CalendarDay;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private List<CalendarDay> calendarDays;
    private List<Cycle> cycles;
    private static final int DEFAULT_CYCLE_LENGTH = 28;
    private Cycle selectedCycle; // Para modificar un ciclo existente

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cycles = new ArrayList<>();
        calendarDays = new ArrayList<>();
        setupCalendarView();
    }

    private void setupCalendarView() {
        calendarView = findViewById(R.id.calendarView);

        Calendar minDate = Calendar.getInstance();
        minDate.add(Calendar.MONTH, -6);
        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.MONTH, 6);

        calendarView.setMinimumDate(minDate);
        calendarView.setMaximumDate(maxDate);

        calendarView.setOnCalendarDayClickListener(this::showDayOptionsDialog);
    }

    private void showDayOptionsDialog(CalendarDay clickedDay) {
        Calendar selectedDate = clickedDay.getCalendar();
        selectedCycle = findCycleForDate(selectedDate);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_day_options, null);

        TextView dateText = dialogView.findViewById(R.id.selectedDateText);
        Button btnStartCycle = dialogView.findViewById(R.id.btnStartCycle);
        Button btnEndCycle = dialogView.findViewById(R.id.btnEndCycle);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        dateText.setText(getString(R.string.selected_date, dateFormat.format(selectedDate.getTime())));

        // Actualizar texto de botones si estamos modificando un ciclo existente
        if (selectedCycle != null) {
            btnStartCycle.setText(R.string.change_cycle_start);
            btnEndCycle.setText(R.string.change_cycle_end);
        }

        AlertDialog dialog = builder.setView(dialogView).create();

        btnStartCycle.setOnClickListener(v -> {
            markCycleStart(selectedDate);
            dialog.dismiss();
        });

        btnEndCycle.setOnClickListener(v -> {
            markCycleEnd(selectedDate);
            dialog.dismiss();
        });

        dialog.show();
    }

    private Cycle findCycleForDate(Calendar date) {
        for (Cycle cycle : cycles) {
            if (cycle.containsDate(date)) {
                return cycle;
            }
        }
        return null;
    }

    private void markCycleStart(Calendar startDate) {
        if (selectedCycle != null) {
            // Si la fecha es posterior al fin del ciclo, no permitir
            if (selectedCycle.getEndDate() != null && startDate.after(selectedCycle.getEndDate())) {
                Toast.makeText(this, "La fecha de inicio no puede ser posterior al fin del ciclo", Toast.LENGTH_SHORT).show();
                return;
            }
            selectedCycle.setStartDate(startDate);
        } else {
            // Crear nuevo ciclo
            Cycle newCycle = new Cycle(startDate, null);
            cycles.add(newCycle);
            selectedCycle = newCycle;
        }
        updateCalendarMarkers();
    }

    private void markCycleEnd(Calendar endDate) {
        if (selectedCycle == null || selectedCycle.getStartDate() == null) {
            Toast.makeText(this, "Por favor, marca primero el inicio del ciclo", Toast.LENGTH_SHORT).show();
            return;
        }

        if (endDate.before(selectedCycle.getStartDate())) {
            Toast.makeText(this, "La fecha de fin no puede ser anterior al inicio", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedCycle.setEndDate(endDate);
        updateCalendarMarkers();
        selectedCycle = null; // Reset selección actual
    }

    private void updateCalendarMarkers() {
        calendarDays.clear();

        // Encontrar el último ciclo completado (con fecha de fin)
        Cycle lastCompleteCycle = null;
        for (Cycle cycle : cycles) {
            if (cycle.getEndDate() != null) {
                if (lastCompleteCycle == null ||
                        cycle.getEndDate().after(lastCompleteCycle.getEndDate())) {
                    lastCompleteCycle = cycle;
                }
            }
        }

        // Marcar todos los ciclos
        for (Cycle cycle : cycles) {
            if (cycle.getStartDate() != null) {
                if (cycle.getEndDate() != null) {
                    // Si tiene fecha de fin, marcar todo el rango
                    Calendar currentDate = (Calendar) cycle.getStartDate().clone();
                    Calendar endDate = cycle.getEndDate();

                    while (!currentDate.after(endDate)) {
                        CalendarDay day = new CalendarDay((Calendar) currentDate.clone());
                        day.setBackgroundResource(R.color.period_day);
                        day.setLabelColor(R.color.white);
                        calendarDays.add(day);

                        currentDate.add(Calendar.DAY_OF_MONTH, 1);
                    }
                } else {
                    // Si solo tiene fecha de inicio, marcar solo ese día
                    CalendarDay day = new CalendarDay((Calendar) cycle.getStartDate().clone());
                    day.setBackgroundResource(R.color.period_day);
                    day.setLabelColor(R.color.white);
                    calendarDays.add(day);
                }
            }
        }

        // Predecir solo después del último ciclo completado
        if (lastCompleteCycle != null) {
            predictNextCycle(lastCompleteCycle);
        }

        calendarView.setCalendarDays(calendarDays);
    }

    private void predictNextCycle(Cycle cycle) {
        // Calcular la duración del ciclo actual
        long cycleLength = (cycle.getEndDate().getTimeInMillis() - cycle.getStartDate().getTimeInMillis()) /
                (24 * 60 * 60 * 1000);

        // Predecir próximo inicio
        Calendar nextPredictedStart = (Calendar) cycle.getStartDate().clone();
        nextPredictedStart.add(Calendar.DAY_OF_MONTH, DEFAULT_CYCLE_LENGTH);

        CalendarDay predictedDay = new CalendarDay(nextPredictedStart);
        predictedDay.setBackgroundResource(R.color.predicted_day);
        predictedDay.setLabelColor(R.color.white);
        calendarDays.add(predictedDay);
    }
}