package com.example.ciclomenstrual.domain

import com.example.ciclomenstrual.domain.model.ContraceptiveRegimen
import com.example.ciclomenstrual.domain.model.PillIntakeStatus

object PillAlarmPolicy {
    fun isProcessable(
        regimen: ContraceptiveRegimen,
        date: Long,
        number: Int,
        event: PillAlarmEvent,
        now: Long,
        intakeStatus: PillIntakeStatus?,
    ): Boolean {
        val calculator = PillScheduleCalculator()
        if (calculator.pillNumber(regimen, date) != number) return false
        if (intakeStatus in COMPLETED_STATUSES) return false

        val placebo = calculator.isPlacebo(regimen, number)
        val dose = calculator.doseTime(regimen, date)
        val deadline = dose + PillStatusResolver.REMINDER_WINDOW_MILLIS
        return when {
            placebo -> event == PillAlarmEvent.DOSE && now >= dose
            now >= deadline -> event == PillAlarmEvent.DEADLINE
            now < dose -> event == PillAlarmEvent.DOSE
            else -> event == PillAlarmEvent.DOSE || event == PillAlarmEvent.REMINDER
        }
    }

    fun isCurrentAction(
        regimen: ContraceptiveRegimen,
        date: Long,
        number: Int,
        today: Long,
    ): Boolean {
        val calculator = PillScheduleCalculator()
        return date == today &&
            calculator.pillNumber(regimen, date) == number &&
            !calculator.isPlacebo(regimen, number)
    }

    private val COMPLETED_STATUSES = setOf(
        PillIntakeStatus.TAKEN,
        PillIntakeStatus.MISSED,
        PillIntakeStatus.AUTO_PLACEBO,
    )
}
