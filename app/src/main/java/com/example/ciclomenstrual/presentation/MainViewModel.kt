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
import com.example.ciclomenstrual.domain.repository.ContraceptiveRepository
import com.example.ciclomenstrual.domain.PillScheduleCalculator
import com.example.ciclomenstrual.domain.PillStatusResolver
import com.example.ciclomenstrual.domain.model.ContraceptiveRegimen
import com.example.ciclomenstrual.domain.model.PillDayStatus
import com.example.ciclomenstrual.domain.model.PillIntake
import com.example.ciclomenstrual.domain.model.PillIntakeSource
import com.example.ciclomenstrual.domain.model.PillIntakeStatus
import com.example.ciclomenstrual.notifications.CycleReminderScheduler
import com.example.ciclomenstrual.notifications.PillReminderScheduler
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.Calendar

class MainViewModel(
    private val cycleRepository: CycleRepository,
    private val noteRepository: NoteRepository,
    private val markerFactory: CalendarMarkerFactory,
    private val reminderScheduler: CycleReminderScheduler,
    private val contraceptiveRepository: ContraceptiveRepository,
    private val pillReminderScheduler: PillReminderScheduler,
    private val pillCalculator: PillScheduleCalculator = PillScheduleCalculator(),
    private val pillStatusResolver: PillStatusResolver = PillStatusResolver(pillCalculator),
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val mutableState = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = mutableState.asStateFlow()
    private val eventChannel = Channel<MainUiEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        load()
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                refreshDerivedPillState()
            }
        }
    }

    fun selectDate(date: Long) {
        val normalized = DateNormalizer.normalize(date)
        val state = mutableState.value
        val selectedDateNotes = state.notes.filter { DateNormalizer.normalize(it.date) == normalized }
        val selectedPillDay = state.pillDays.firstOrNull { it.date == normalized }
        if (state.selectedDate == normalized &&
            state.selectedDateNotes == selectedDateNotes &&
            state.selectedPillDay == selectedPillDay
        ) return

        mutableState.value = state.copy(
            selectedDate = normalized,
            selectedDateNotes = selectedDateNotes,
            selectedPillDay = selectedPillDay,
        )
    }

    fun startNewRegimen(startDate: Long) = viewModelScope.launch {
        val normalized = DateNormalizer.normalize(startDate)
        if (normalized > DateNormalizer.normalize(now())) {
            message("La fecha de la pastilla 1 no puede ser futura")
            return@launch
        }
        if (mutableState.value.activeRegimen?.let { normalized <= it.startDate } == true) {
            message("El nuevo tratamiento debe empezar después del tratamiento actual")
            return@launch
        }
        val draft = ContraceptiveRegimen(startDate = normalized)
        val initial = buildInitialIntakes(draft, now())
        contraceptiveRepository.startRegimen(draft, initial)
        reloadContraceptiveState()
        message("Tratamiento configurado")
    }

    fun markPillTaken(day: com.example.ciclomenstrual.domain.model.PillDay) = viewModelScope.launch {
        if (day.date > DateNormalizer.normalize(now()) || day.isPlacebo) return@launch
        contraceptiveRepository.saveIntake(
            PillIntake(
                day.regimenId,
                day.date,
                day.pillNumber,
                PillIntakeStatus.TAKEN,
                now(),
                PillIntakeSource.CALENDAR,
            ),
        )
        reloadContraceptiveState()
    }

    fun unmarkPill(day: com.example.ciclomenstrual.domain.model.PillDay) = viewModelScope.launch {
        if (day.date > DateNormalizer.normalize(now()) || day.isPlacebo) return@launch
        contraceptiveRepository.deleteIntake(day.regimenId, day.date)
        reloadContraceptiveState(reconcile = false)
    }

    fun refreshAlarmPermission() {
        mutableState.value = mutableState.value.copy(
            exactAlarmAvailable = pillReminderScheduler.canScheduleExact(),
        )
        schedulePillReminder()
    }

    fun refreshContraceptiveState() = viewModelScope.launch {
        reloadContraceptiveState()
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
            val regimens = contraceptiveRepository.getRegimens()
            var intakes = contraceptiveRepository.getIntakes()
            val activeRegimen = regimens.lastOrNull { it.endDate == null }
            if (activeRegimen != null) {
                reconcileMissingHistory(activeRegimen, intakes, now())
                intakes = contraceptiveRepository.getIntakes()
            }
            val pillDays = buildPillDays(regimens, intakes, now())
            val selectedCycle = cycles.lastOrNull()?.takeIf { it.endDate == null }
            mutableState.value = MainUiState(
                isLoading = false,
                cycles = cycles,
                notes = notes,
                selectedCycle = selectedCycle,
                regimens = regimens,
                activeRegimen = activeRegimen,
                pillIntakes = intakes,
                pillDays = pillDays,
                exactAlarmAvailable = pillReminderScheduler.canScheduleExact(),
            )
            rebuildMarkers()
            schedulePillReminder()
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
            mutableState.value.pillDays,
        )
        mutableState.value = mutableState.value.copy(markers = markers, nextPredictedDay = prediction)
    }

    private suspend fun message(text: String) = eventChannel.send(MainUiEvent.Message(text))

    private suspend fun reloadContraceptiveState(reconcile: Boolean = true) {
        val regimens = contraceptiveRepository.getRegimens()
        val active = regimens.lastOrNull { it.endDate == null }
        var intakes = contraceptiveRepository.getIntakes()
        if (reconcile && active != null) {
            reconcileMissingHistory(active, intakes, now())
            intakes = contraceptiveRepository.getIntakes()
        }
        val days = buildPillDays(regimens, intakes, now())
        mutableState.value = mutableState.value.copy(
            regimens = regimens,
            activeRegimen = active,
            pillIntakes = intakes,
            pillDays = days,
            selectedPillDay = mutableState.value.selectedDate?.let { date ->
                days.firstOrNull { it.date == date }
            },
            exactAlarmAvailable = pillReminderScheduler.canScheduleExact(),
        )
        rebuildMarkers()
        schedulePillReminder()
    }

    private fun schedulePillReminder() {
        val state = mutableState.value
        state.activeRegimen?.let {
            pillReminderScheduler.scheduleNext(it, state.pillIntakes, now())
        } ?: pillReminderScheduler.cancel()
    }

    private fun refreshDerivedPillState() {
        val state = mutableState.value
        if (state.regimens.isEmpty()) return
        val days = buildPillDays(state.regimens, state.pillIntakes, now())
        mutableState.value = state.copy(
            pillDays = days,
            selectedPillDay = state.selectedDate?.let { date -> days.firstOrNull { it.date == date } },
        )
        rebuildMarkers()
    }

    private fun buildInitialIntakes(regimen: ContraceptiveRegimen, currentTime: Long): List<PillIntake> {
        val today = DateNormalizer.normalize(currentTime)
        val result = mutableListOf<PillIntake>()
        var date = regimen.startDate
        while (date < today) {
            val number = pillCalculator.pillNumber(regimen, date)!!
            val placebo = pillCalculator.isPlacebo(regimen, number)
            result += PillIntake(
                0,
                date,
                number,
                if (placebo) PillIntakeStatus.AUTO_PLACEBO else PillIntakeStatus.TAKEN,
                pillCalculator.doseTime(regimen, date),
                PillIntakeSource.INITIALIZATION,
            )
            date = DateNormalizer.addDays(date, 1)
        }
        return result
    }

    private suspend fun reconcileMissingHistory(
        regimen: ContraceptiveRegimen,
        existing: List<PillIntake>,
        currentTime: Long,
    ) {
        val existingDates = existing.filter { it.regimenId == regimen.id }.mapTo(hashSetOf()) { it.scheduledDate }
        val today = DateNormalizer.normalize(currentTime)
        var date = regimen.startDate
        while (date <= today) {
            if (date !in existingDates) {
                val number = pillCalculator.pillNumber(regimen, date) ?: break
                val dose = pillCalculator.doseTime(regimen, date)
                val placebo = pillCalculator.isPlacebo(regimen, number)
                val status = when {
                    placebo && currentTime >= dose -> PillIntakeStatus.AUTO_PLACEBO
                    !placebo && currentTime >= dose + PillStatusResolver.REMINDER_WINDOW_MILLIS ->
                        PillIntakeStatus.MISSED
                    else -> null
                }
                status?.let {
                    contraceptiveRepository.saveIntake(
                        PillIntake(
                            regimen.id,
                            date,
                            number,
                            it,
                            currentTime.takeIf { _ -> placebo },
                            PillIntakeSource.SYSTEM,
                        ),
                    )
                }
            }
            date = DateNormalizer.addDays(date, 1)
        }
    }

    private fun buildPillDays(
        regimens: List<ContraceptiveRegimen>,
        intakes: List<PillIntake>,
        currentTime: Long,
    ): List<com.example.ciclomenstrual.domain.model.PillDay> {
        val min = Calendar.getInstance().apply {
            timeInMillis = currentTime
            add(Calendar.MONTH, -6)
        }.timeInMillis.let(DateNormalizer::normalize)
        val max = Calendar.getInstance().apply {
            timeInMillis = currentTime
            add(Calendar.MONTH, 6)
        }.timeInMillis.let(DateNormalizer::normalize)
        val intakeMap = intakes.associateBy { it.regimenId to it.scheduledDate }
        val result = mutableListOf<com.example.ciclomenstrual.domain.model.PillDay>()
        var date = min
        while (date <= max) {
            val regimen = regimens.lastOrNull {
                date >= it.startDate && (it.endDate == null || date <= it.endDate)
            }
            regimen?.let {
                pillStatusResolver.resolve(it, date, intakeMap[it.id to date], currentTime)?.let(result::add)
            }
            date = DateNormalizer.addDays(date, 1)
        }
        return result
    }

    class Factory(
        private val cycleRepository: CycleRepository,
        private val noteRepository: NoteRepository,
        private val markerFactory: CalendarMarkerFactory,
        private val reminderScheduler: CycleReminderScheduler,
        private val contraceptiveRepository: ContraceptiveRepository,
        private val pillReminderScheduler: PillReminderScheduler,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(
                cycleRepository,
                noteRepository,
                markerFactory,
                reminderScheduler,
                contraceptiveRepository,
                pillReminderScheduler,
            ) as T
    }
}
