package com.example.ciclomenstrual.domain

import com.example.ciclomenstrual.domain.model.ContraceptiveRegimen
import com.example.ciclomenstrual.domain.model.PillDay
import com.example.ciclomenstrual.domain.model.PillDayStatus
import com.example.ciclomenstrual.domain.model.PillIntake
import com.example.ciclomenstrual.domain.model.PillIntakeStatus

class PillStatusResolver(
    private val calculator: PillScheduleCalculator = PillScheduleCalculator(),
) {
    fun resolve(
        regimen: ContraceptiveRegimen,
        date: Long,
        intake: PillIntake?,
        now: Long,
    ): PillDay? {
        val day = date
        val number = calculator.pillNumber(regimen, day) ?: return null
        val placebo = calculator.isPlacebo(regimen, number)
        val status = when (intake?.status) {
            PillIntakeStatus.TAKEN -> PillDayStatus.TAKEN
            PillIntakeStatus.MISSED -> PillDayStatus.MISSED
            PillIntakeStatus.AUTO_PLACEBO -> PillDayStatus.AUTO_PLACEBO
            null -> {
                val dose = calculator.doseTime(regimen, day)
                val deadline = dose + REMINDER_WINDOW_MILLIS
                when {
                    placebo && now >= dose -> PillDayStatus.AUTO_PLACEBO
                    day > DateNormalizer.todayKey(now) || now < dose -> PillDayStatus.UPCOMING
                    now < deadline -> PillDayStatus.PENDING
                    else -> PillDayStatus.MISSED
                }
            }
        }
        return PillDay(regimen.id, day, number, placebo, status, intake?.takenAt)
    }

    companion object {
        const val REMINDER_WINDOW_MILLIS = 2 * 60 * 60 * 1000L
    }
}
