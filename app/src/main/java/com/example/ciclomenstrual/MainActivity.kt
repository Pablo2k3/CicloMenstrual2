package com.example.ciclomenstrual

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.applandeo.materialcalendarview.CalendarDay
import com.example.ciclomenstrual.data.local.AppDatabase
import com.example.ciclomenstrual.data.repository.RoomCycleRepository
import com.example.ciclomenstrual.data.repository.RoomNoteRepository
import com.example.ciclomenstrual.databinding.ActivityMainBinding
import com.example.ciclomenstrual.databinding.DialogDayOptionsBinding
import com.example.ciclomenstrual.domain.CalendarMarkerFactory
import com.example.ciclomenstrual.domain.model.CalendarMarker
import com.example.ciclomenstrual.domain.model.Cycle
import com.example.ciclomenstrual.domain.model.MarkerBackground
import com.example.ciclomenstrual.domain.model.Note
import com.example.ciclomenstrual.notifications.NotificationHelper
import com.example.ciclomenstrual.notifications.WorkManagerCycleReminderScheduler
import com.example.ciclomenstrual.presentation.MainUiEvent
import com.example.ciclomenstrual.presentation.MainUiState
import com.example.ciclomenstrual.presentation.MainViewModel
import com.example.ciclomenstrual.presentation.NotesAdapter
import com.example.ciclomenstrual.presentation.CalendarInterop
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var notesAdapter: NotesAdapter
    private val viewModel: MainViewModel by viewModels {
        val database = AppDatabase.getInstance(applicationContext)
        MainViewModel.Factory(
            RoomCycleRepository(database.cycleDao()),
            RoomNoteRepository(database.noteDao()),
            CalendarMarkerFactory(),
            WorkManagerCycleReminderScheduler(applicationContext),
        )
    }
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupCalendar()
        setupNotes()
        setupActions()
        observeState()
        NotificationHelper.createChannel(this)
        requestNotificationPermission()
    }

    private fun setupCalendar() {
        CalendarInterop.configureRange(
            binding.calendarView,
            Calendar.getInstance().apply { add(Calendar.MONTH, -6) },
            Calendar.getInstance().apply { add(Calendar.MONTH, 6) },
        ) {
            viewModel.selectDate(it.calendar.timeInMillis)
        }
    }

    private fun setupNotes() {
        notesAdapter = NotesAdapter(::confirmNoteDeletion)
        binding.notesRecyclerView.apply {
            adapter = notesAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }
    }

    private fun setupActions() {
        binding.fab.setOnClickListener {
            val date = CalendarInterop.selectedDate(binding.calendarView)
            if (date == null) {
                Toast.makeText(this, "Selecciona un día en el calendario", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.selectDate(date.timeInMillis)
                showDayOptions(date.timeInMillis)
            }
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect {
                        notesAdapter.submitList(it.selectedDateNotes)
                        renderMarkers(it)
                    }
                }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is MainUiEvent.Message ->
                                Toast.makeText(this@MainActivity, event.text, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun renderMarkers(state: MainUiState) {
        binding.calendarView.setCalendarDays(state.markers.map(::toCalendarDay))
    }

    private fun toCalendarDay(marker: CalendarMarker): CalendarDay {
        val calendarDay = CalendarDay(Calendar.getInstance().apply { timeInMillis = marker.date })
        when (marker.background) {
            MarkerBackground.PERIOD -> CalendarInterop.setBackground(calendarDay, R.color.period_day)
            MarkerBackground.ONGOING -> CalendarInterop.setBackground(calendarDay, R.color.ongoing_day)
            MarkerBackground.PREDICTED -> CalendarInterop.setBackground(calendarDay, R.color.predicted_day)
            MarkerBackground.NONE -> Unit
        }
        if (marker.background != MarkerBackground.NONE) {
            CalendarInterop.setLabelColor(calendarDay, R.color.white)
        }
        calendarDay.imageDrawable = markerIcon(marker)
        return calendarDay
    }

    private fun markerIcon(marker: CalendarMarker): Drawable? {
        val note = CalendarInterop.textDrawable(this, "📝", R.color.black, 13)
        val warning = CalendarInterop.textDrawable(this, "⚠️", R.color.red, 13)
        return when {
            marker.hasNote && marker.overduePrediction -> LayerDrawable(arrayOf(note, warning)).apply {
                setLayerInset(1, 0, 0, 40, 0)
                setLayerInset(0, 40, 0, 0, 0)
            }
            marker.hasNote -> note
            marker.overduePrediction -> warning
            else -> null
        }
    }

    private fun showDayOptions(date: Long) {
        val dialogBinding = DialogDayOptionsBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this).setView(dialogBinding.root).create()
        dialogBinding.selectedDateText.text = getString(
            R.string.selected_date,
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date),
        )
        val completeCycle = viewModel.cycleForDate(date)
        if (completeCycle != null) {
            dialogBinding.btnStartCycle.setText(R.string.delete_cycle)
            dialogBinding.btnStartCycle.setOnClickListener {
                viewModel.deleteCycle(completeCycle)
                dialog.dismiss()
            }
            dialogBinding.btnEndCycle.visibility = View.GONE
        } else {
            if (viewModel.state.value.selectedCycle != null) {
                dialogBinding.btnStartCycle.setText(R.string.change_cycle_start)
            } else {
                dialogBinding.btnStartCycle.setText(R.string.mark_cycle_start)
            }
            dialogBinding.btnEndCycle.setText(R.string.mark_cycle_end)
            dialogBinding.btnStartCycle.setOnClickListener {
                viewModel.markCycleStart(date)
                dialog.dismiss()
            }
            dialogBinding.btnEndCycle.setOnClickListener {
                viewModel.markCycleEnd(date)
                dialog.dismiss()
            }
        }
        dialogBinding.btnAddNote.setOnClickListener {
            dialog.dismiss()
            showAddNote(date)
        }
        dialog.show()
    }

    private fun showAddNote(date: Long) {
        val input = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("Añadir nota")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ -> viewModel.addNote(date, input.text.toString()) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmNoteDeletion(note: Note) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar nota")
            .setMessage("¿Estás seguro de que quieres eliminar esta nota?")
            .setPositiveButton("Eliminar") { _, _ -> viewModel.deleteNote(note) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
