package com.example.ciclomenstrual.presentation

import com.example.ciclomenstrual.domain.CalendarMarkerFactory
import com.example.ciclomenstrual.domain.DateNormalizer
import com.example.ciclomenstrual.domain.model.ContraceptiveRegimen
import com.example.ciclomenstrual.domain.model.Cycle
import com.example.ciclomenstrual.domain.model.Note
import com.example.ciclomenstrual.domain.model.PillDay
import com.example.ciclomenstrual.domain.model.PillDayStatus
import com.example.ciclomenstrual.domain.model.PillIntake
import com.example.ciclomenstrual.domain.repository.ContraceptiveRepository
import com.example.ciclomenstrual.domain.repository.CycleRepository
import com.example.ciclomenstrual.domain.repository.NoteRepository
import com.example.ciclomenstrual.notifications.CycleReminderScheduler
import com.example.ciclomenstrual.notifications.PillReminderScheduler
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val madrid = TimeZone.getTimeZone(DateNormalizer.REFERENCE_TIME_ZONE_ID)
    private val today = date(2026, Calendar.JULY, 23)
    private val currentTime = DateNormalizer.toLocalTimestamp(today, TimeZone.getDefault()) + 12 * 60 * 60 * 1_000
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `changing cycle start deletes the old key`() = runTest(dispatcher.scheduler) {
        val oldStart = DateNormalizer.addDays(today, -1)
        val cycles = FakeCycleRepository(mutableListOf(Cycle(oldStart)))
        val viewModel = createViewModel(cycles = cycles)
        advanceUntilIdle()

        viewModel.markCycleStart(today)
        advanceUntilIdle()

        assertEquals(listOf(oldStart), cycles.deletedStarts)
        assertEquals(listOf(today), viewModel.state.value.cycles.map { it.startDate })
    }

    @Test
    fun `completing a cycle schedules the updated prediction`() = runTest(dispatcher.scheduler) {
        val start = DateNormalizer.addDays(today, -1)
        val cycles = FakeCycleRepository(mutableListOf(Cycle(start)))
        val reminders = FakeCycleReminderScheduler()
        val viewModel = createViewModel(cycles = cycles, reminderScheduler = reminders)
        advanceUntilIdle()

        viewModel.markCycleEnd(today)
        advanceUntilIdle()

        assertEquals(DateNormalizer.addDays(start, 28), reminders.scheduled.last())
        assertTrue(reminders.scheduled.size >= 2)
    }

    @Test
    fun `deleting a note removes only its persistent identity`() = runTest(dispatcher.scheduler) {
        val first = Note(id = 10, date = today, content = "igual")
        val second = Note(id = 11, date = today, content = "igual")
        val notes = FakeNoteRepository(mutableListOf(first, second))
        val viewModel = createViewModel(notes = notes)
        advanceUntilIdle()

        viewModel.selectDate(DateNormalizer.toLocalTimestamp(today))
        viewModel.deleteNote(first)
        advanceUntilIdle()

        assertEquals(listOf(10L), notes.deletedIds)
        assertEquals(listOf(11L), viewModel.state.value.notes.map { it.id })
        assertEquals(listOf(second), viewModel.state.value.selectedDateNotes)
    }

    @Test
    fun `manual pill confirmation cancels the visible notification`() = runTest(dispatcher.scheduler) {
        val regimen = ContraceptiveRegimen(id = 7, startDate = today)
        val contraceptive = FakeContraceptiveRepository(regimen)
        val pills = FakePillReminderScheduler()
        val viewModel = createViewModel(
            contraceptive = contraceptive,
            pillReminderScheduler = pills,
        )
        advanceUntilIdle()

        viewModel.markPillTaken(
            PillDay(
                regimenId = regimen.id,
                date = today,
                pillNumber = 1,
                isPlacebo = false,
                status = PillDayStatus.PENDING,
                takenAt = null,
            ),
        )
        advanceUntilIdle()

        assertEquals(1, pills.cancelNotificationCalls)
        assertEquals(1, contraceptive.savedIntakes.size)
    }

    private fun createViewModel(
        cycles: FakeCycleRepository = FakeCycleRepository(),
        notes: FakeNoteRepository = FakeNoteRepository(),
        contraceptive: FakeContraceptiveRepository = FakeContraceptiveRepository(),
        reminderScheduler: FakeCycleReminderScheduler = FakeCycleReminderScheduler(),
        pillReminderScheduler: FakePillReminderScheduler = FakePillReminderScheduler(),
    ) = MainViewModel(
        cycleRepository = cycles,
        noteRepository = notes,
        markerFactory = CalendarMarkerFactory(),
        reminderScheduler = reminderScheduler,
        contraceptiveRepository = contraceptive,
        pillReminderScheduler = pillReminderScheduler,
        now = { currentTime },
        startRefreshLoop = false,
    )

    private fun date(year: Int, month: Int, day: Int): Long = Calendar.getInstance(madrid).apply {
        clear()
        set(year, month, day, 0, 0, 0)
    }.timeInMillis.let { DateNormalizer.normalize(it, madrid) }
}

private class FakeCycleRepository(
    private val stored: MutableList<Cycle> = mutableListOf(),
) : CycleRepository {
    val deletedStarts = mutableListOf<Long>()

    override suspend fun getAll(): List<Cycle> = stored.toList()
    override suspend fun insert(cycle: Cycle) {
        stored.removeAll { it.startDate == cycle.startDate }
        stored += cycle
    }
    override suspend fun updateEnd(startDate: Long, endDate: Long) {
        val index = stored.indexOfFirst { it.startDate == startDate }
        if (index >= 0) stored[index] = stored[index].copy(endDate = endDate)
    }
    override suspend fun delete(cycle: Cycle) {
        stored.remove(cycle)
    }
    override suspend fun deleteByStartDate(startDate: Long) {
        deletedStarts += startDate
        stored.removeAll { it.startDate == startDate }
    }
    override suspend fun exists(startDate: Long): Boolean = stored.any { it.startDate == startDate }
}

private class FakeNoteRepository(
    private val stored: MutableList<Note> = mutableListOf(),
) : NoteRepository {
    val deletedIds = mutableListOf<Long>()

    override suspend fun getAll(): List<Note> = stored.toList()
    override suspend fun insert(note: Note): Long {
        val id = (stored.maxOfOrNull { it.id } ?: 0) + 1
        stored += note.copy(id = id)
        return id
    }
    override suspend fun deleteById(id: Long) {
        deletedIds += id
        stored.removeAll { it.id == id }
    }
}

private class FakeContraceptiveRepository(
    private val active: ContraceptiveRegimen? = null,
) : ContraceptiveRepository {
    val savedIntakes = mutableListOf<PillIntake>()
    private val intakes = mutableListOf<PillIntake>()

    override suspend fun getRegimens(): List<ContraceptiveRegimen> = active?.let(::listOf).orEmpty()
    override suspend fun getActiveRegimen(): ContraceptiveRegimen? = active
    override suspend fun getIntakes(): List<PillIntake> = intakes.toList()
    override suspend fun startRegimen(regimen: ContraceptiveRegimen, initialIntakes: List<PillIntake>): Long = 1
    override suspend fun saveIntake(intake: PillIntake) {
        savedIntakes += intake
        intakes.removeAll { it.regimenId == intake.regimenId && it.scheduledDate == intake.scheduledDate }
        intakes += intake
    }
    override suspend fun deleteIntake(regimenId: Long, scheduledDate: Long) {
        intakes.removeAll { it.regimenId == regimenId && it.scheduledDate == scheduledDate }
    }
}

private class FakeCycleReminderScheduler : CycleReminderScheduler {
    val scheduled = mutableListOf<Long>()
    var cancelCalls = 0

    override fun schedule(predictedCycleStart: Long) {
        scheduled += predictedCycleStart
    }
    override fun cancel() {
        cancelCalls++
    }
}

private class FakePillReminderScheduler : PillReminderScheduler {
    var cancelNotificationCalls = 0

    override fun scheduleNext(regimen: ContraceptiveRegimen, intakes: List<PillIntake>, now: Long) = Unit
    override fun cancel() = Unit
    override fun cancelCurrentNotification() {
        cancelNotificationCalls++
    }
    override fun canScheduleExact(): Boolean = true
}
