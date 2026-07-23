package com.example.ciclomenstrual.domain

import com.example.ciclomenstrual.domain.model.Cycle

enum class CycleValidationError {
    START_IN_FUTURE,
    END_IN_FUTURE,
    START_REQUIRED,
    END_BEFORE_START,
    OVERLAPPING_CYCLE,
}

object CycleRules {
    fun validateStart(start: Long, now: Long): CycleValidationError? =
        CycleValidationError.START_IN_FUTURE.takeIf { start > now }

    fun validateEnd(
        cycle: Cycle?,
        end: Long,
        now: Long,
        nextCycle: Cycle?,
    ): CycleValidationError? = when {
        end > now -> CycleValidationError.END_IN_FUTURE
        cycle == null -> CycleValidationError.START_REQUIRED
        end < cycle.startDate -> CycleValidationError.END_BEFORE_START
        nextCycle?.startDate?.let { it < end } == true -> CycleValidationError.OVERLAPPING_CYCLE
        else -> null
    }

    fun isOngoing(cycle: Cycle, cycles: List<Cycle>, now: Long): Boolean =
        cycle.endDate == null &&
            cycles.lastOrNull() == cycle &&
            DateNormalizer.daysBetween(cycle.startDate, now) < 7
}
