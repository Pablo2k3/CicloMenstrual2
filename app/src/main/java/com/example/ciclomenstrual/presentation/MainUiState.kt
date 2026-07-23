package com.example.ciclomenstrual.presentation

import com.example.ciclomenstrual.domain.model.CalendarMarker
import com.example.ciclomenstrual.domain.model.Cycle
import com.example.ciclomenstrual.domain.model.Note

data class MainUiState(
    val isLoading: Boolean = true,
    val selectedDate: Long? = null,
    val cycles: List<Cycle> = emptyList(),
    val notes: List<Note> = emptyList(),
    val selectedDateNotes: List<Note> = emptyList(),
    val markers: List<CalendarMarker> = emptyList(),
    val selectedCycle: Cycle? = null,
    val nextPredictedDay: Long? = null,
)

sealed interface MainUiEvent {
    data class Message(val text: String) : MainUiEvent
}
