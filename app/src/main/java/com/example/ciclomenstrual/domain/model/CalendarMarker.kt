package com.example.ciclomenstrual.domain.model

enum class MarkerBackground { NONE, PERIOD, ONGOING, PREDICTED }

data class CalendarMarker(
    val date: Long,
    val background: MarkerBackground = MarkerBackground.NONE,
    val hasNote: Boolean = false,
    val overduePrediction: Boolean = false,
    val pillDay: PillDay? = null,
)
