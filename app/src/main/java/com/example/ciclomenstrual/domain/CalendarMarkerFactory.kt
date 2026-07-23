package com.example.ciclomenstrual.domain

import com.example.ciclomenstrual.domain.model.CalendarMarker
import com.example.ciclomenstrual.domain.model.Cycle
import com.example.ciclomenstrual.domain.model.MarkerBackground
import com.example.ciclomenstrual.domain.model.Note
import com.example.ciclomenstrual.domain.model.PillDay

class CalendarMarkerFactory(
    private val predictionPolicy: CyclePredictionPolicy = FixedCyclePredictionPolicy(),
) {
    fun create(
        cycles: List<Cycle>,
        notes: List<Note>,
        now: Long,
        pillDays: List<PillDay> = emptyList(),
    ): Pair<List<CalendarMarker>, Long?> {
        val markers = linkedMapOf<Long, CalendarMarker>()
        val notesByDay = notes.groupBy { DateNormalizer.normalize(it.date) }

        cycles.forEach { cycle ->
            val end = cycle.endDate
            if (end != null) {
                addRange(markers, cycle.startDate, end, MarkerBackground.PERIOD, notesByDay)
            } else {
                put(markers, cycle.startDate, MarkerBackground.PERIOD, notesByDay)
                if (CycleRules.isOngoing(cycle, cycles, now)) {
                    val tomorrow = DateNormalizer.addDays(cycle.startDate, 1)
                    addRange(markers, tomorrow, now, MarkerBackground.ONGOING, notesByDay)
                }
            }
        }

        val predicted = cycles.lastOrNull()?.let(predictionPolicy::predict)
        predicted?.let {
            val day = DateNormalizer.normalize(it)
            markers[day] = CalendarMarker(
                date = day,
                background = MarkerBackground.PREDICTED,
                hasNote = notesByDay[day].orEmpty().isNotEmpty(),
                overduePrediction = it < now,
            )
        }
        notesByDay.filterValues { it.isNotEmpty() }.keys.forEach { day ->
            markers[day] = (markers[day] ?: CalendarMarker(day)).copy(hasNote = true)
        }
        pillDays.forEach { pill ->
            markers[pill.date] = (markers[pill.date] ?: CalendarMarker(pill.date)).copy(pillDay = pill)
        }
        return markers.values.toList() to predicted
    }

    private fun addRange(
        target: MutableMap<Long, CalendarMarker>,
        start: Long,
        end: Long,
        background: MarkerBackground,
        notes: Map<Long, List<Note>>,
    ) {
        var day = DateNormalizer.normalize(start)
        val last = DateNormalizer.normalize(end)
        while (day <= last) {
            put(target, day, background, notes)
            day = DateNormalizer.addDays(day, 1)
        }
    }

    private fun put(
        target: MutableMap<Long, CalendarMarker>,
        date: Long,
        background: MarkerBackground,
        notes: Map<Long, List<Note>>,
    ) {
        val day = DateNormalizer.normalize(date)
        target[day] = CalendarMarker(day, background, notes[day].orEmpty().isNotEmpty())
    }
}
