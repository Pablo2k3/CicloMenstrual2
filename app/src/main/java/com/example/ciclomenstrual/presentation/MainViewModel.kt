package com.example.ciclomenstrual.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ciclomenstrual.domain.CalendarMarkerFactory
import com.example.ciclomenstrual.domain.CycleRules
import com.example.ciclomenstrual.domain.CycleValidationError
import com.example.ciclomenstrual.domain.DateNormalizer
import com.example.ciclomenstrual.domain.model.Cycle
import com.example.ciclomenstrual.domain.model.Note
import com.example.ciclomenstrual.domain.repository.CycleRepository
import com.example.ciclomenstrual.domain.repository.NoteRepository
import com.example.ciclomenstrual.notifications.CycleReminderScheduler
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val cycleRepository: CycleRepository,
    private val noteRepository: NoteRepository,
    private val markerFactory: CalendarMarkerFactory,
    private val reminderScheduler: CycleReminderScheduler,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val mutableState = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = mutableState.asStateFlow()
    private val eventChannel = Channel<MainUiEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        load()
    }

    fun selectDate(date: Long) {
        val normalized = DateNormalizer.normalize(date)
        mutableState.value = mutableState.value.copy(
            selectedDate = normalized,
            selectedDateNotes = mutableState.value.notes.filter { DateNormalizer.normalize(it.date) == normalized },
        )
    }

    fun cycleForDate(date: Long): Cycle? =
        mutableState.value.cycles.firstOrNull { it.endDate != null && it.contains(date) }

    fun markCycleStart(date: Long) = viewModelScope.launch {
        if (CycleRules.validateStart(date, now()) != null) {
            message("La fecha de inicio no puede ser posterior a hoy")
            return@launch
        }
        val oldSelected = mutableState.value.selectedCycle
        var cycles = mutableState.value.cycles.toMutableList()
        if (oldSelected != null) {
            cycles.remove(oldSelected)
            // Preserves the legacy lookup semantics for compatibility.
            cycleRepository.deleteByStartDate(date)
        }
        val cycle = Cycle(date)
        cycles.add(cycle)
        cycles = cycles.sortedBy { it.startDate }.toMutableList()
        if (CycleRules.isOngoing(cycle, cycles, now())) cycleRepository.insert(cycle)
        mutableState.value = mutableState.value.copy(cycles = cycles, selectedCycle = cycle)
        rebuildMarkers()
    }

    fun markCycleEnd(date: Long) = viewModelScope.launch {
        val state = mutableState.value
        val selected = state.selectedCycle
        val index = state.cycles.indexOf(selected)
        val next = state.cycles.getOrNull(index + 1)
        when (CycleRules.validateEnd(selected, date, now(), next)) {
            CycleValidationError.END_IN_FUTURE -> message("La fecha de fin no puede ser posterior a hoy")
            CycleValidationError.START_REQUIRED -> message("Por favor, marca primero el inicio del ciclo")
            CycleValidationError.END_BEFORE_START -> message("La fecha de fin no puede ser anterior al inicio")
            CycleValidationError.OVERLAPPING_CYCLE -> {
                message("Ya existe un ciclo en ese rango de fechas")
                val cycles = state.cycles.toMutableList().apply { selected?.let(::remove) }
                mutableState.value = state.copy(cycles = cycles, selectedCycle = null)
                rebuildMarkers()
            }
            else -> {
                val completed = selected!!.copy(endDate = date)
                if (cycleRepository.exists(completed.startDate)) {
                    cycleRepository.updateEnd(completed.startDate, date)
                } else {
                    cycleRepository.insert(completed)
                }
                val cycles = state.cycles.map { if (it == selected) completed else it }
                mutableState.value = state.copy(cycles = cycles, selectedCycle = null)
                rebuildMarkers()
                if (cycles.lastOrNull() == completed) {
                    mutableState.value.nextPredictedDay?.let(reminderScheduler::schedule)
                }
            }
        }
    }

    fun deleteCycle(cycle: Cycle) = viewModelScope.launch {
        val wasLast = mutableState.value.cycles.lastOrNull() == cycle
        cycleRepository.delete(cycle)
        mutableState.value = mutableState.value.copy(
            cycles = mutableState.value.cycles - cycle,
            selectedCycle = mutableState.value.selectedCycle.takeUnless { it == cycle },
        )
        if (wasLast) reminderScheduler.cancel()
        rebuildMarkers()
        if (wasLast) mutableState.value.nextPredictedDay?.let(reminderScheduler::schedule)
        message("Ciclo eliminado")
    }

    fun addNote(date: Long, content: String) = viewModelScope.launch {
        if (content.trim().isEmpty()) return@launch
        val note = Note(date = DateNormalizer.normalize(date), content = content)
        noteRepository.insert(note)
        val notes = mutableState.value.notes + note
        mutableState.value = mutableState.value.copy(notes = notes)
        selectDate(date)
        rebuildMarkers()
    }

    fun deleteNote(note: Note) = viewModelScope.launch {
        noteRepository.deleteByDateAndContent(note.date, note.content)
        // The DAO removes all duplicates; mirror that result in memory.
        val notes = mutableState.value.notes.filterNot {
            it.date == note.date && it.content == note.content
        }
        mutableState.value = mutableState.value.copy(notes = notes)
        mutableState.value.selectedDate?.let(::selectDate)
        rebuildMarkers()
    }

    private fun load() = viewModelScope.launch {
        runCatching {
            val cycles = cycleRepository.getAll()
            val notes = noteRepository.getAll()
            val selectedCycle = cycles.lastOrNull()?.takeIf { it.endDate == null }
            mutableState.value = MainUiState(
                isLoading = false,
                cycles = cycles,
                notes = notes,
                selectedCycle = selectedCycle,
            )
            rebuildMarkers()
        }.onFailure {
            mutableState.value = mutableState.value.copy(isLoading = false)
            message("No se pudieron cargar los datos")
        }
    }

    private fun rebuildMarkers() {
        val (markers, prediction) = markerFactory.create(
            mutableState.value.cycles,
            mutableState.value.notes,
            now(),
        )
        mutableState.value = mutableState.value.copy(markers = markers, nextPredictedDay = prediction)
    }

    private suspend fun message(text: String) = eventChannel.send(MainUiEvent.Message(text))

    class Factory(
        private val cycleRepository: CycleRepository,
        private val noteRepository: NoteRepository,
        private val markerFactory: CalendarMarkerFactory,
        private val reminderScheduler: CycleReminderScheduler,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(cycleRepository, noteRepository, markerFactory, reminderScheduler) as T
    }
}
