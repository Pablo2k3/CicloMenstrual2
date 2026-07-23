package com.example.ciclomenstrual.domain.model

enum class PillIntakeStatus { TAKEN, MISSED, AUTO_PLACEBO }
enum class PillIntakeSource { NOTIFICATION, CALENDAR, INITIALIZATION, SYSTEM }

data class PillIntake(
    val regimenId: Long,
    val scheduledDate: Long,
    val pillNumber: Int,
    val status: PillIntakeStatus,
    val takenAt: Long? = null,
    val source: PillIntakeSource,
)

enum class PillDayStatus { UPCOMING, PENDING, TAKEN, MISSED, AUTO_PLACEBO }

data class PillDay(
    val regimenId: Long,
    val date: Long,
    val pillNumber: Int,
    val isPlacebo: Boolean,
    val status: PillDayStatus,
    val takenAt: Long? = null,
)
