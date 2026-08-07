package com.example.ciclomenstrual

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import com.example.ciclomenstrual.data.repository.RoomContraceptiveRepository
import com.example.ciclomenstrual.databinding.ActivityMainBinding
import com.example.ciclomenstrual.databinding.DialogDayOptionsBinding
import com.example.ciclomenstrual.domain.CalendarMarkerFactory
import com.example.ciclomenstrual.domain.DateNormalizer
import com.example.ciclomenstrual.domain.model.CalendarMarker
import com.example.ciclomenstrual.domain.model.ContraceptiveRegimen
import com.example.ciclomenstrual.domain.model.Cycle
import com.example.ciclomenstrual.domain.model.MarkerBackground
import com.example.ciclomenstrual.domain.model.Note
import com.example.ciclomenstrual.notifications.NotificationHelper
import com.example.ciclomenstrual.notifications.WorkManagerCycleReminderScheduler
import com.example.ciclomenstrual.notifications.AlarmManagerPillReminderScheduler
import com.example.ciclomenstrual.presentation.MainUiEvent
import com.example.ciclomenstrual.presentation.MainViewModel
import com.example.ciclomenstrual.presentation.NotesAdapter
import com.example.ciclomenstrual.presentation.CalendarInterop
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.ciclomenstrual.domain.model.PillDay
import com.example.ciclomenstrual.domain.model.PillDayStatus

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
            RoomContraceptiveRepository(database),
            AlarmManagerPillReminderScheduler(applicationContext),
        )
    }
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    private var onboardingShown = false

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
            binding.calendarSelectionOverlay.showSelection()
            viewModel.selectDate(it.calendar.timeInMillis)
        }
        CalendarInterop.setSurfaceColor(
            binding.calendarView,
            ContextCompat.getColor(this, R.color.app_surface),
        )
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
            val date = viewModel.state.value.selectedDate
            if (date == null) {
                Toast.makeText(this, "Selecciona un día en el calendario", Toast.LENGTH_SHORT).show()
            } else {
                showDayOptions(date)
            }
        }
        binding.newTreatmentButton.setOnClickListener { confirmNewTreatment() }
        binding.exactAlarmWarning.setOnClickListener { requestExactAlarmPermission() }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state
                        .map { it.selectedDateNotes }
                        .distinctUntilChanged()
                        .collect(notesAdapter::submitList)
                }
                launch {
                    viewModel.state
                        .map { it.selectedDate }
                        .distinctUntilChanged()
                        .collect(::renderSelectedDate)
                }
                launch {
                    viewModel.state
                        .map { it.markers }
                        .distinctUntilChanged()
                        .collect(::renderMarkers)
                }
                launch {
                    viewModel.state
                        .map { PillCardUiState(it.activeRegimen, it.selectedPillDay, it.exactAlarmAvailable) }
                        .distinctUntilChanged()
                        .collect(::renderPillCard)
                }
                launch {
                    viewModel.state
                        .map { it.isLoading to it.activeRegimen }
                        .distinctUntilChanged()
                        .collect { (isLoading, activeRegimen) ->
                            if (!isLoading && activeRegimen == null && !onboardingShown) {
                                onboardingShown = true
                                showRegimenDatePicker(isInitial = true)
                            }
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

    private fun renderMarkers(markers: List<CalendarMarker>) {
        binding.calendarView.setCalendarDays(markers.map(::toCalendarDay))
    }

    private fun renderSelectedDate(date: Long?) {
        binding.selectedDayLabel.visibility = if (date == null) View.GONE else View.VISIBLE
        binding.selectedDayLabel.text = date?.let {
            getString(
                R.string.selected_date,
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(
                    DateNormalizer.toLocalTimestamp(it),
                ),
            )
        }.orEmpty()
    }

    private fun toCalendarDay(marker: CalendarMarker): CalendarDay {
        val calendarDay = CalendarDay(
            Calendar.getInstance().apply {
                timeInMillis = DateNormalizer.toLocalTimestamp(marker.date)
            },
        )
        when (marker.background) {
            MarkerBackground.PERIOD -> CalendarInterop.setBackground(calendarDay, R.color.period_day)
            MarkerBackground.ONGOING -> CalendarInterop.setBackground(calendarDay, R.color.ongoing_day)
            MarkerBackground.PREDICTED -> CalendarInterop.setBackground(calendarDay, R.color.predicted_day)
            MarkerBackground.NONE -> Unit
        }
        val isPastMissedPill = marker.pillDay?.let {
            it.status == PillDayStatus.MISSED &&
                it.date < DateNormalizer.todayKey()
        } == true
        if (isPastMissedPill) {
            CalendarInterop.setLabelColor(calendarDay, R.color.pill_missed_day)
        } else if (marker.background != MarkerBackground.NONE) {
            CalendarInterop.setLabelColor(calendarDay, R.color.white)
        }
        calendarDay.imageDrawable = markerIcon(marker)
        return calendarDay
    }

    private fun markerIcon(marker: CalendarMarker): Drawable? {
        if (!marker.hasNote && !marker.overduePrediction) return null

        val layers = mutableListOf<Drawable>()
        if (marker.hasNote) {
            layers += CalendarInterop.textDrawable(this, "📝", R.color.calendar_note_icon, 13)
        }
        if (marker.overduePrediction) {
            layers += CalendarInterop.textDrawable(this, "⚠️", R.color.calendar_warning_icon, 13)
        }
        if (layers.size == 1) return layers.single()
        return LayerDrawable(layers.toTypedArray()).apply {
            layers.indices.forEach { index ->
                setLayerInset(index, index * 24, 0, (layers.size - index - 1) * 24, 0)
            }
        }
    }

    private fun renderPillCard(state: PillCardUiState) {
        binding.exactAlarmWarning.visibility =
            if (state.activeRegimen != null && !state.exactAlarmAvailable) View.VISIBLE else View.GONE
        val day = state.selectedPillDay
        binding.pillCard.visibility = View.VISIBLE
        if (state.activeRegimen == null) {
            binding.pillTitle.setText(R.string.configure_pills)
            binding.pillStatus.setText(R.string.select_pill_one_date)
            binding.pillActionButton.setText(R.string.configure_pills)
            binding.pillActionButton.visibility = View.VISIBLE
            binding.pillActionButton.setOnClickListener { showRegimenDatePicker(isInitial = true) }
            binding.newTreatmentButton.visibility = View.GONE
            return
        }
        binding.newTreatmentButton.visibility = View.VISIBLE
        if (day == null) {
            binding.pillTitle.setText(R.string.select_day_for_pill)
            binding.pillStatus.text = ""
            binding.pillActionButton.visibility = View.GONE
            return
        }
        binding.pillTitle.text = getString(
            if (day.isPlacebo) R.string.pill_placebo else R.string.pill_active,
            day.pillNumber,
        )
        binding.pillStatus.text = getString(R.string.pill_status, pillStatusText(day.status))
        val editable = !day.isPlacebo && day.date <= DateNormalizer.todayKey()
        binding.pillActionButton.visibility = if (editable) View.VISIBLE else View.GONE
        if (editable) {
            val taken = day.status == PillDayStatus.TAKEN
            binding.pillActionButton.setText(if (taken) R.string.unmark_taken else R.string.mark_taken)
            binding.pillActionButton.setOnClickListener {
                if (taken) viewModel.unmarkPill(day) else viewModel.markPillTaken(day)
            }
        }
    }

    private data class PillCardUiState(
        val activeRegimen: ContraceptiveRegimen?,
        val selectedPillDay: PillDay?,
        val exactAlarmAvailable: Boolean,
    )

    private fun pillStatusText(status: PillDayStatus): String = getString(
        when (status) {
            PillDayStatus.UPCOMING -> R.string.pill_upcoming_status
            PillDayStatus.PENDING -> R.string.pill_pending_status
            PillDayStatus.TAKEN -> R.string.pill_taken_status
            PillDayStatus.MISSED -> R.string.pill_missed_status
            PillDayStatus.AUTO_PLACEBO -> R.string.pill_placebo_status
        },
    )

    private fun confirmNewTreatment() {
        AlertDialog.Builder(this)
            .setTitle("Comenzar nuevo tratamiento")
            .setMessage("El tratamiento actual se cerrará el día anterior. Su historial se conservará.")
            .setPositiveButton("Continuar") { _, _ -> showRegimenDatePicker(isInitial = false) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showRegimenDatePicker(isInitial: Boolean) {
        val today = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val date = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                viewModel.startNewRegimen(date.timeInMillis)
                if (isInitial) explainReminderPermissions()
            },
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DAY_OF_MONTH),
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
            setTitle("Fecha de la pastilla 1")
        }.show()
    }

    private fun explainReminderPermissions() {
        AlertDialog.Builder(this)
            .setTitle("Recordatorios diarios")
            .setMessage(
                "La app avisará a las 14:00 en verano y a las 13:00 en invierno " +
                    "según la hora peninsular española. Para que sea puntual, " +
                    "permite Alarmas y recordatorios.",
            )
            .setPositiveButton("Configurar") { _, _ -> requestExactAlarmPermission() }
            .setNegativeButton("Más tarde", null)
            .show()
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:$packageName"),
                ),
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            viewModel.refreshAlarmPermission()
            if (!viewModel.state.value.isLoading) viewModel.refreshContraceptiveState()
        }
    }

    private fun showDayOptions(date: Long) {
        val dialogBinding = DialogDayOptionsBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this).setView(dialogBinding.root).create()
        dialogBinding.selectedDateText.text = getString(
            R.string.selected_date,
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(
                DateNormalizer.toLocalTimestamp(date),
            ),
        )
        val completeCycle = viewModel.cycleForDate(date)
        if (completeCycle != null) {
            dialogBinding.btnStartCycle.setText(R.string.delete_cycle)
            dialogBinding.btnStartCycle.setOnClickListener {
                dialog.dismiss()
                AlertDialog.Builder(this)
                    .setTitle(R.string.confirm_delete_cycle_title)
                    .setMessage(R.string.confirm_delete_cycle_message)
                    .setPositiveButton(R.string.delete_cycle) { _, _ ->
                        viewModel.deleteCycle(completeCycle)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
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
