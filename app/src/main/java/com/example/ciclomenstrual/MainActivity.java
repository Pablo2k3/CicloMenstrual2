package com.example.ciclomenstrual;

import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.applandeo.materialcalendarview.CalendarDay;
import com.applandeo.materialcalendarview.CalendarUtils;
import com.applandeo.materialcalendarview.CalendarView;
import com.example.ciclomenstrual.DayNotesManager.RemovedNote;
import com.example.ciclomenstrual.database.RoomCycle;
import com.example.ciclomenstrual.NotificationHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.lifecycle.ViewModelProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements NotesAdapter.OnNoteDeletedListener {

    private static final int DEFAULT_CYCLE_LENGTH = 28;
    private static final int ONGOING_CYCLE_DAYS = 7;
    private static final String NEXT_CYCLE_MESSAGE = "Tu próximo ciclo está previsto para mañana";

    private CalendarView calendarView;
    private final List<CalendarDay> calendarDays = new ArrayList<>();
    private final CycleStore cycleStore = new CycleStore();
    private DayNotesManager dayNotesManager;
    private NotesAdapter adapter;
    private Calendar selectedDate;
    private MainViewModel viewModel;
    private CycleNotificationScheduler notificationScheduler;
    private Calendar nextPredictedDay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        dayNotesManager = new DayNotesManager();
        setupCalendarView();
        setupNotesList();

        viewModel.getCycles().observe(this, cycles -> {
            cycleStore.setInitialCycles(cycles);
            updateCalendarMarkers(false);
        });

        viewModel.getNotes().observe(this, notes -> {
            dayNotesManager.loadFromEntities(notes);
            if (selectedDate != null) {
                adapter.updateNotes(dayNotesManager.getNotesForDate(selectedDate));
            }
            updateCalendarMarkers(false);
        });

        viewModel.getPredictedNextCycle().observe(this, prediction -> {
            // Prediction is handled inside updateCalendarMarkers via buildNextPrediction
            // but we can also use this if we want to separate logic.
            // For now, updateCalendarMarkers calls buildNextPrediction internally using
            // cycleStore.
            // So we just trigger updateCalendarMarkers when cycles change.
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            Calendar markedDate = calendarView.getFirstSelectedDate();
            if (markedDate != null) {
                showDayOptionsDialog(new CalendarDay(markedDate));
            } else {
                Toast.makeText(this, "Selecciona un día en el calendario", Toast.LENGTH_SHORT).show();
            }
        });

        // loadInitialData removed, handled by ViewModel init
        notificationScheduler = new CycleNotificationScheduler(this);
        NotificationHelper.createNotificationChannel(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission();
        }
    }

    private void setupNotesList() {
        RecyclerView notesRecyclerView = findViewById(R.id.notesRecyclerView);
        adapter = new NotesAdapter(new ArrayList<>(), this);
        notesRecyclerView.setAdapter(adapter);
        notesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration decoration = new DividerItemDecoration(notesRecyclerView.getContext(),
                LinearLayoutManager.VERTICAL);
        notesRecyclerView.addItemDecoration(decoration);
    }

    // loadInitialData removed

    private void requestNotificationPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { android.Manifest.permission.POST_NOTIFICATIONS },
                    1);
        }
    }

    private void setupCalendarView() {
        calendarView = findViewById(R.id.calendarView);

        Calendar minDate = Calendar.getInstance();
        minDate.add(Calendar.MONTH, -6);
        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.MONTH, 6);

        calendarView.setMinimumDate(minDate);
        calendarView.setMaximumDate(maxDate);

        calendarView.setOnCalendarDayClickListener(eventDay -> {
            selectedDate = dayNotesManager.normalize(eventDay.getCalendar());
            List<String> notesForDate = dayNotesManager.getNotesForDate(selectedDate);
            if (adapter != null) {
                adapter.updateNotes(notesForDate);
            }
        });
    }

    private void deleteCycle(Cycle cycle) {
        if (cycle == null) {
            return;
        }
        viewModel.deleteCycle(cycle);
        boolean wasLastCycle = cycleStore.isLastCycle(cycle);
        if (wasLastCycle) {
            notificationScheduler.cancelAllNotifications();
        }
        Toast.makeText(this, "Ciclo eliminado", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNoteDeleted(int position) {
        if (selectedDate == null) {
            return;
        }
        RemovedNote removedNote = dayNotesManager.removeNote(selectedDate, position);
        if (removedNote == null) {
            return;
        }
        selectedDate = removedNote.getDate();
        viewModel.deleteNote(selectedDate, removedNote.getContent());
    }

    private void showDayOptionsDialog(CalendarDay clickedDay) {
        Calendar dayCalendar = dayNotesManager.normalize(clickedDay.getCalendar());
        selectedDate = (Calendar) dayCalendar.clone();

        List<String> notesForDate = dayNotesManager.getNotesForDate(dayCalendar);
        RecyclerView notesRecyclerView = findViewById(R.id.notesRecyclerView);
        NotesAdapter currentAdapter = (NotesAdapter) notesRecyclerView.getAdapter();
        if (currentAdapter != null) {
            currentAdapter.updateNotes(notesForDate);
        }

        Cycle cycleForDate = cycleStore.findCycleForDate(dayCalendar);
        Cycle selectedCycle = cycleStore.getSelectedCycle();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_day_options, null);

        TextView dateText = dialogView.findViewById(R.id.selectedDateText);
        Button btnStartCycle = dialogView.findViewById(R.id.btnStartCycle);
        Button btnEndCycle = dialogView.findViewById(R.id.btnEndCycle);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        dateText.setText(getString(R.string.selected_date, dateFormat.format(dayCalendar.getTime())));

        AlertDialog dialog = builder.setView(dialogView).create();

        if (cycleForDate != null && cycleForDate.getStartDate() != null && cycleForDate.getEndDate() != null) {
            btnStartCycle.setText(R.string.delete_cycle);
            btnStartCycle.setOnClickListener(v -> {
                deleteCycle(cycleForDate);
                dialog.dismiss();
            });
            btnEndCycle.setVisibility(View.GONE);
        } else {
            if (selectedCycle != null) {
                btnStartCycle.setText(R.string.change_cycle_start);
                btnEndCycle.setText(R.string.mark_cycle_end);
            } else {
                btnStartCycle.setText(R.string.mark_cycle_start);
                btnEndCycle.setText(R.string.mark_cycle_end);
            }

            btnStartCycle.setOnClickListener(v -> {
                markCycleStart((Calendar) dayCalendar.clone());
                dialog.dismiss();
            });

            btnEndCycle.setOnClickListener(v -> {
                markCycleEnd((Calendar) dayCalendar.clone());
                dialog.dismiss();
            });
        }

        Button btnAddNote = dialogView.findViewById(R.id.btnAddNote);
        btnAddNote.setOnClickListener(v -> {
            showAddNoteDialog(dayCalendar);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showAddNoteDialog(Calendar selectedCalendar) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Añadir nota");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String noteContent = input.getText().toString();
            if (!noteContent.trim().isEmpty()) {
                Calendar normalizedDate = dayNotesManager.normalize(selectedCalendar);
                viewModel.addNote(normalizedDate, noteContent);
                selectedDate = (Calendar) normalizedDate.clone();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void markCycleStart(Calendar startDate) {
        Calendar today = Calendar.getInstance();
        if (CycleCalculator.isDateInFuture(startDate, today)) {
            Toast.makeText(this, "La fecha de inicio no puede ser posterior a hoy", Toast.LENGTH_SHORT).show();
            return;
        }

        Cycle existingSelectedCycle = cycleStore.getSelectedCycle();
        if (existingSelectedCycle != null) {
            viewModel.deleteCycle(existingSelectedCycle);
            cycleStore.clearSelection();
            markCycleStart(startDate);
            return;
        }

        Cycle newCycle = new Cycle((Calendar) startDate.clone(), null);

        // Check if it's the last cycle (conceptually)
        Cycle last = cycleStore.getLastCycle();
        boolean isLast = last == null || newCycle.getStartDate().after(last.getStartDate());

        if (isLast && CycleCalculator.isOngoingCycle(newCycle, ONGOING_CYCLE_DAYS, Calendar.getInstance())) {
            viewModel.addCycle(newCycle);
        } else {
            Toast.makeText(this, "Solo se pueden añadir ciclos recientes", Toast.LENGTH_SHORT).show();
        }
    }

    private void markCycleEnd(Calendar endDate) {
        Cycle selectedCycle = cycleStore.getSelectedCycle();
        if (selectedCycle == null || selectedCycle.getStartDate() == null) {
            Toast.makeText(this, "Por favor, marca primero el inicio del ciclo", Toast.LENGTH_SHORT).show();
            return;
        }

        if (CycleCalculator.isDateInFuture(endDate, Calendar.getInstance())) {
            Toast.makeText(this, "La fecha de fin no puede ser posterior a hoy", Toast.LENGTH_SHORT).show();
            return;
        }

        if (CycleCalculator.isEndDateBeforeStart(selectedCycle.getStartDate(), endDate)) {
            Toast.makeText(this, "La fecha de fin no puede ser anterior al inicio", Toast.LENGTH_SHORT).show();
            return;
        }

        if (cycleStore.hasOverlappingCycle(selectedCycle, endDate)) {
            Toast.makeText(this, "Ya existe un ciclo en ese rango de fechas", Toast.LENGTH_SHORT).show();
            viewModel.deleteCycle(selectedCycle);
            cycleStore.clearSelection();
            return;
        }

        selectedCycle.setEndDate((Calendar) endDate.clone());
        viewModel.updateCycle(selectedCycle);
        cycleStore.clearSelection();
    }

    private void updateCalendarMarkers(boolean scheduleNotification) {
        nextPredictedDay = null;
        calendarDays.clear();

        Drawable noteDrawable = CalendarUtils.getDrawableText(this, "\uD83D\uDCDD", null, R.color.black, 13);
        Drawable noteOnlyDrawable = CalendarUtils.getDrawableText(this, "\uD83D\uDCDD", null, R.color.black, 13);
        Drawable alertIcon = CalendarUtils.getDrawableText(this, "⚠\uFE0F", null, R.color.red, 13);

        LayerDrawable noteAndAlert = new LayerDrawable(new Drawable[] {
                CalendarUtils.getDrawableText(this, "\uD83D\uDCDD", null, R.color.black, 10),
                CalendarUtils.getDrawableText(this, "⚠\uFE0F", null, R.color.red, 10)
        });
        noteAndAlert.setLayerInset(1, 0, 0, 40, 0);
        noteAndAlert.setLayerInset(0, 40, 0, 0, 0);

        for (Cycle cycle : cycleStore.getCycles()) {
            if (cycle.getStartDate() == null) {
                continue;
            }
            if (cycle.getEndDate() != null) {
                Calendar currentDate = (Calendar) cycle.getStartDate().clone();
                Calendar endDate = cycle.getEndDate();
                while (!currentDate.after(endDate)) {
                    CalendarDay day = new CalendarDay((Calendar) currentDate.clone());
                    day.setBackgroundResource(R.color.period_day);
                    day.setLabelColor(R.color.white);
                    if (dayNotesManager.hasNotes(day.getCalendar())) {
                        day.setImageDrawable(noteDrawable);
                    }
                    calendarDays.add(day);
                    currentDate.add(Calendar.DAY_OF_MONTH, 1);
                }
            } else {
                CalendarDay startDay = new CalendarDay((Calendar) cycle.getStartDate().clone());
                startDay.setBackgroundResource(R.color.period_day);
                startDay.setLabelColor(R.color.white);
                if (dayNotesManager.hasNotes(startDay.getCalendar())) {
                    startDay.setImageDrawable(noteDrawable);
                }
                calendarDays.add(startDay);

                if (CycleCalculator.isOngoingCycle(cycle, ONGOING_CYCLE_DAYS, Calendar.getInstance())) {
                    Calendar currentDate = (Calendar) cycle.getStartDate().clone();
                    currentDate.add(Calendar.DAY_OF_MONTH, 1);
                    Calendar today = Calendar.getInstance();
                    while (!currentDate.after(today)) {
                        CalendarDay ongoingDay = new CalendarDay((Calendar) currentDate.clone());
                        ongoingDay.setBackgroundResource(R.color.ongoing_day);
                        ongoingDay.setLabelColor(R.color.white);
                        if (dayNotesManager.hasNotes(ongoingDay.getCalendar())) {
                            ongoingDay.setImageDrawable(noteDrawable);
                        }
                        calendarDays.add(ongoingDay);
                        currentDate.add(Calendar.DAY_OF_MONTH, 1);
                    }
                }
            }
        }

        Cycle lastCompleteCycle = cycleStore.getLastCompleteCycle();
        nextPredictedDay = buildNextPrediction(lastCompleteCycle, scheduleNotification);

        if (nextPredictedDay != null) {
            CalendarDay predictedDay = new CalendarDay(nextPredictedDay);
            predictedDay.setBackgroundResource(R.color.predicted_day);
            predictedDay.setLabelColor(R.color.white);
            if (dayNotesManager.hasNotes(predictedDay.getCalendar())) {
                predictedDay.setImageDrawable(noteDrawable);
            }
            calendarDays.add(predictedDay);

            Calendar today = Calendar.getInstance();
            if (nextPredictedDay.before(today)) {
                if (dayNotesManager.hasNotes(nextPredictedDay)) {
                    predictedDay.setImageDrawable(noteAndAlert);
                } else {
                    predictedDay.setImageDrawable(alertIcon);
                }
            }
        }

        for (Calendar dateWithNotes : dayNotesManager.getAllDatesWithNotes()) {
            if (dayNotesManager.isEmpty(dateWithNotes)) {
                continue;
            }
            CalendarDay day = new CalendarDay(dateWithNotes);
            if (nextPredictedDay == null || !dateWithNotes.equals(nextPredictedDay)) {
                day.setImageDrawable(noteOnlyDrawable);
            }
            calendarDays.add(day);
        }

        calendarView.setCalendarDays(calendarDays);
    }

    private Calendar buildNextPrediction(Cycle lastCompleteCycle, boolean scheduleNotification) {
        if (lastCompleteCycle == null) {
            return null;
        }
        Calendar predictedStart = CycleCalculator.predictNextCycleStart(lastCompleteCycle, DEFAULT_CYCLE_LENGTH);
        if (predictedStart == null) {
            return null;
        }
        if (scheduleNotification) {
            Calendar notificationDate = (Calendar) predictedStart.clone();
            notificationDate.add(Calendar.DAY_OF_MONTH, -1);
            Calendar todayEnd = CycleCalculator.endOfDay(Calendar.getInstance());
            if (predictedStart.after(todayEnd)) {
                notificationScheduler.cancelAllNotifications();
                notificationScheduler.scheduleNotification(notificationDate, 8, 15, NEXT_CYCLE_MESSAGE);
            }
        }
        return predictedStart;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
