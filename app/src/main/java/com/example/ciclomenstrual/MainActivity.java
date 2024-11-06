package com.example.ciclomenstrual;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.applandeo.materialcalendarview.CalendarUtils;
import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.CalendarDay;
import com.applandeo.materialcalendarview.EventDay;
import com.example.ciclomenstrual.database.AppDatabase;
import com.example.ciclomenstrual.database.CycleDao;
import com.example.ciclomenstrual.database.Note;
import com.example.ciclomenstrual.database.NoteDao;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import androidx.lifecycle.LifecycleOwner;

public class MainActivity extends AppCompatActivity implements NotesAdapter.OnNoteDeletedListener {

    private CalendarView calendarView;
    private List<CalendarDay> calendarDays;
    private List<Cycle> cycles;
    private static final int DEFAULT_CYCLE_LENGTH = 28;
    private Cycle selectedCycle; // Para modificar un ciclo existente
    private HashMap<Calendar, List<String>> dayNotes = new HashMap<>();
    private NotesAdapter adapter;
    private Calendar selectedDate;
    private AppDatabase db;
    private CycleDao cycleDao; // Declarar cycleDao
    private NoteDao noteDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Borra la base de datos
        //this.deleteDatabase("app_database");
        //
        // Obtener una instancia de AppDatabase
        db = AppDatabase.getInstance(this);
        cycleDao = db.cycleDao();
        noteDao = db.noteDao();

        cycles = new ArrayList<>(); // Inicializar la lista cycles
        calendarDays = new ArrayList<>();
        setupCalendarView();

        // Inicializar el RecyclerView y el adaptador
        RecyclerView notesRecyclerView = findViewById(R.id.notesRecyclerView);
        adapter = new NotesAdapter(new ArrayList<>(), this);
        notesRecyclerView.setAdapter(adapter);
        notesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(notesRecyclerView.getContext(),
                LinearLayoutManager.VERTICAL);
        notesRecyclerView.addItemDecoration(dividerItemDecoration);

        // Agregar listener al FloatingActionButton
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            // Obtener el día marcado
            Calendar selectedDate = calendarView.getFirstSelectedDate();

            // Verificar si hay un día marcado
            if (selectedDate != null) {
                // Mostrar el diálogo con las opciones
                showDayOptionsDialog(new CalendarDay(selectedDate));
            } else {
                // Mostrar un mensaje al usuario indicando que no hay un día marcado
                Toast.makeText(this, "Selecciona un día en el calendario", Toast.LENGTH_SHORT).show();
            }
        });

        // Cargar tanto los ciclos como las notas
        executor.execute(() -> {
            List<com.example.ciclomenstrual.database.Cycle> cyclesDB = cycleDao.getAllCycles();
            for (com.example.ciclomenstrual.database.Cycle cycleDB : cyclesDB) {
                cycles.add(CycleConverter.fromRoomCycle(cycleDB));
            }

            // Cargar notas
            List<Note> notesDB = noteDao.getAllNotes();
            NoteConverter.loadNotesIntoMap(dayNotes, notesDB);

            runOnUiThread(this::updateCalendarMarkers);
        });
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
            // Guardar la fecha seleccionada
            selectedDate = eventDay.getCalendar();

            // Obtener las notas para la fecha seleccionada
            List<String> notesForDate = dayNotes.get(selectedDate);

            // Actualizar el adaptador del RecyclerView con las notas
            if (adapter != null) {
                adapter.updateNotes(notesForDate != null ? notesForDate : new ArrayList<>());
            }
        });
    }

    private void deleteCycle(Cycle cycle) {
        if (cycle != null) {
            executor.execute(() -> {
                cycleDao.deleteCycle(CycleConverter.toRoomCycle(cycle));
                runOnUiThread(() -> {
                    cycles.remove(cycle);
                    updateCalendarMarkers();
                    Toast.makeText(this, "Ciclo eliminado", Toast.LENGTH_SHORT).show();
                });
            });
        }
    }

    // Implementación de la interfaz OnNoteDeletedListener
    @Override
    public void onNoteDeleted(int position) {
        Context context = this;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Eliminar nota");
        builder.setMessage("¿Estás seguro de que quieres eliminar esta nota?");

        builder.setPositiveButton("Eliminar", (dialog, which) -> {
            List<String> notesForDate = dayNotes.get(selectedDate);
            if (notesForDate != null && position < notesForDate.size()) {
                String noteContent = notesForDate.get(position);

                // Eliminar de la base de datos
                executor.execute(() -> {
                    noteDao.deleteNoteByDateAndContent(
                            NoteConverter.dateToTimestamp(selectedDate),
                            noteContent
                    );
                    runOnUiThread(() -> {
                        // Actualizar el HashMap local
                        notesForDate.remove(position);
                        dayNotes.put(selectedDate, notesForDate);

                        // Actualizar la UI
                        adapter.updateNotes(notesForDate);
                    });
                });
            }
            dialog.dismiss();
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
    private void showDayOptionsDialog(CalendarDay clickedDay) {
        Calendar selectedDate = clickedDay.getCalendar();

        // Obtener la lista de notas para la fecha seleccionada
        List<String> notesForDate = dayNotes.get(selectedDate);

        // Añadir log para debug
        System.out.println("Mostrando notas para la fecha " + selectedDate.getTime() + ": " + notesForDate);

        // Mostrar las notas en el RecyclerView
        RecyclerView notesRecyclerView = findViewById(R.id.notesRecyclerView);
        NotesAdapter adapter = (NotesAdapter) notesRecyclerView.getAdapter();
        if (adapter != null) {
            adapter.updateNotes(notesForDate != null ? notesForDate : new ArrayList<>());
        }

        // Buscar el ciclo para la fecha seleccionada (si existe), solo si selectedCycle ya es null
        /*if (selectedCycle == null) {
            selectedCycle = findCycleForDate(selectedDate);
        }*/
        Cycle cycleBorrar = findCycleForDate(selectedDate);


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_day_options, null);

        TextView dateText = dialogView.findViewById(R.id.selectedDateText);
        Button btnStartCycle = dialogView.findViewById(R.id.btnStartCycle);
        Button btnEndCycle = dialogView.findViewById(R.id.btnEndCycle);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        dateText.setText(getString(R.string.selected_date, dateFormat.format(selectedDate.getTime())));

        AlertDialog dialog = builder.setView(dialogView).create();

        // Actualizar texto de botones o agregar botón "Eliminar ciclo"
        if (cycleBorrar != null && cycleBorrar.getStartDate() != null && cycleBorrar.getEndDate() != null) {
            // Si la fecha pertenece a un ciclo completo, mostrar opción para eliminarlo
            btnStartCycle.setText(R.string.delete_cycle); // Reutilizar el botón de inicio
            btnStartCycle.setOnClickListener(v -> {
                deleteCycle(cycleBorrar);
                dialog.dismiss(); // Acceder a dialog desde el ámbito de la clase anónima
            });
            btnEndCycle.setVisibility(View.GONE); // Ocultar el botón de fin
        } else {
            // Si no hay ciclo o solo tiene inicio, mostrar opciones para marcar inicio o fin
            if (selectedCycle != null) {
                // Si hay un ciclo con solo inicio, mostrar opción para cambiar inicio o marcar fin
                btnStartCycle.setText(R.string.change_cycle_start);
                btnEndCycle.setText(R.string.mark_cycle_end);
            } else {
                // Si no hay ciclo, mostrar opciones para marcar inicio o fin
                btnStartCycle.setText(R.string.mark_cycle_start);
                btnEndCycle.setText(R.string.mark_cycle_end);
            }

            btnStartCycle.setOnClickListener(v -> {
                markCycleStart(selectedDate);
                dialog.dismiss();
            });

            btnEndCycle.setOnClickListener(v -> {
                markCycleEnd(selectedDate);
                dialog.dismiss();
            });
        }
        // Agregar la opción de añadir/editar nota
        Button btnAddNote = dialogView.findViewById(R.id.btnAddNote);
        btnAddNote.setOnClickListener(v -> {
            showAddNoteDialog(selectedDate);
            dialog.dismiss();
        });
        dialog.show();
    }

    private Cycle findCycleForDate(Calendar date) {
        for (Cycle cycle : cycles) {
            if (cycle.getStartDate() != null && cycle.getEndDate() != null && cycle.containsDate(date)) {
                return cycle;
            }
        }
        return null;
    }
    private void showAddNoteDialog(Calendar selectedDate) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Añadir nota");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String noteContent = input.getText().toString();
            if (!noteContent.trim().isEmpty()) {
                // Normalizar la fecha
                Calendar normalizedDate = (Calendar) selectedDate.clone();
                normalizedDate.set(Calendar.HOUR_OF_DAY, 0);
                normalizedDate.set(Calendar.MINUTE, 0);
                normalizedDate.set(Calendar.SECOND, 0);
                normalizedDate.set(Calendar.MILLISECOND, 0);

                // Crear nota para la base de datos
                Note newNote = new Note(NoteConverter.dateToTimestamp(normalizedDate), noteContent);

                // Guardar en la base de datos
                executor.execute(() -> {
                    noteDao.insert(newNote);
                    runOnUiThread(() -> {
                        // Actualizar el HashMap local
                        if (!dayNotes.containsKey(normalizedDate)) {
                            dayNotes.put(normalizedDate, new ArrayList<>());
                        }
                        dayNotes.get(normalizedDate).add(noteContent);

                        // Actualizar la UI
                        if (adapter != null) {
                            adapter.updateNotes(dayNotes.get(normalizedDate));
                        }
                        updateCalendarMarkers();
                    });
                });
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }
    private void markCycleStart(Calendar startDate) {
        if (selectedCycle != null) {
            // Actualizar la fecha de inicio del ciclo existente
            selectedCycle.setStartDate(startDate);
        } else {
            // Crear un nuevo ciclo
            Cycle newCycle = new Cycle(startDate, null);
            int indiceCiclo = 0;
            for (Cycle cycle : cycles) {
                if (newCycle.getStartDate().after(cycle.getStartDate())) {
                    indiceCiclo++;
                }
            }
            cycles.add(indiceCiclo, newCycle);
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

        // comprobamos el ciclo siguiente, si existe, para ver si hay ciclos contenidos
        int index = cycles.indexOf(selectedCycle);
        if (index < cycles.size() - 1) {
            Cycle cicloSiguiente = cycles.get(index+1);
            if (cicloSiguiente.getStartDate() != null && cicloSiguiente.getStartDate().before(endDate)){
                // Existe un ciclo contenido, cancelar la creación del nuevo ciclo
                Toast.makeText(this, "Ya existe un ciclo en ese rango de fechas", Toast.LENGTH_SHORT).show();
                cycles.remove(selectedCycle);
                selectedCycle = null; // Resetear la selección del ciclo
                updateCalendarMarkers();
                return; // Salir del método sin crear o actualizar el ciclo
            }
        }

        // Si no hay ciclos contenidos, actualizar el ciclo
        selectedCycle.setEndDate(endDate);

        // Convertir a entidad de Room
        com.example.ciclomenstrual.database.Cycle roomCycle = CycleConverter.toRoomCycle(selectedCycle);

        // Insertar en la base de datos usando executor
        executor.execute(() -> {
            cycleDao.insertCycle(roomCycle);
            runOnUiThread(() -> {
                updateCalendarMarkers();
                selectedCycle = null; // Reset selección actual
            });
        });
    }

    private void updateCalendarMarkers() {
        calendarDays.clear();

        // Encontrar el último ciclo completado (con fecha de fin)
        Cycle lastCompleteCycle = null;
        if (!cycles.isEmpty()){
            lastCompleteCycle = cycles.get(cycles.size() - 1);
        }

        // Marcar todos los ciclos
        for (Cycle cycle : cycles) {
            if (cycle.getStartDate() != null) {
                if (cycle.getEndDate() != null) {
                    // Si tiene fecha de fin, marcar to.do el rango
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
        // Predecir próximo inicio
        Calendar nextPredictedStart = (Calendar) cycle.getStartDate().clone();
        nextPredictedStart.add(Calendar.DAY_OF_MONTH, DEFAULT_CYCLE_LENGTH);

        CalendarDay predictedDay = new CalendarDay(nextPredictedStart);
        predictedDay.setBackgroundResource(R.color.predicted_day);
        predictedDay.setLabelColor(R.color.white);

        // Verificar si la fecha predicha es anterior a la fecha actual
        Calendar currentDate = Calendar.getInstance();
        if (nextPredictedStart.before(currentDate)) {
            // Crear el icono de alerta con una exclamación
            Drawable alertIcon = CalendarUtils.getDrawableText(this, "\uD83D\uDC76\uD83C\uDFFB", null, R.color.red, 13); // Ajusta el color y tamaño según tus preferencias

            // Agregar icono de alerta
            predictedDay.setImageDrawable(alertIcon); // Usar setImageResource() con el Drawable creado
        }

        calendarDays.add(predictedDay);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }
}