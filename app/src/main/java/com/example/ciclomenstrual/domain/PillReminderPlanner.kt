package com.example.ciclomenstrual.domain

import com.example.ciclomenstrual.domain.model.ContraceptiveRegimen
import com.example.ciclomenstrual.domain.model.PillIntake
import com.example.ciclomenstrual.domain.model.PillIntakeStatus

enum class PillAlarmEvent { DOSE, REMINDER, DEADLINE }

data class PlannedPillAlarm(
    val date: Long,
    val pillNumber: Int,
    val event: PillAlarmEvent,
    val triggerAt: Long,
)

class PillReminderPlanner(
    private val calculator: PillScheduleCalculator = PillScheduleCalculator(),
) {
    fun next(
        regimen: ContraceptiveRegimen,
        intakes: List<PillIntake>,
        now: Long,
    ): PlannedPillAlarm? {
        val today = DateNormalizer.normalize(now)
        val number = calculator.pillNumber(regimen, today)
            ?: return dosePlan(regimen, regimen.startDate)
        val intake = intakes.firstOrNull { it.regimenId == regimen.id && it.scheduledDate == today }
        val dose = calculator.doseTime(regimen, today)
        val deadline = dose + PillStatusResolver.REMINDER_WINDOW_MILLIS
        val completed = intake?.status == PillIntakeStatus.TAKEN ||
            intake?.status == PillIntakeStatus.AUTO_PLACEBO ||
            intake?.status == PillIntakeStatus.MISSED
        if (calculator.isPlacebo(regimen, number) && now >= dose || completed) {
            return dosePlan(regimen, DateNormalizer.addDays(today, 1))
        }
        if (now < dose) return PlannedPillAlarm(today, number, PillAlarmEvent.DOSE, dose)
        if (now >= deadline) return PlannedPillAlarm(today, number, PillAlarmEvent.DEADLINE, now + 1_000)
        val interval = 15 * 60 * 1000L
        val next = dose + (((now - dose) / interval) + 1) * interval
        return if (next >= deadline) {
            PlannedPillAlarm(today, number, PillAlarmEvent.DEADLINE, deadline)
        } else {
            PlannedPillAlarm(today, number, PillAlarmEvent.REMINDER, next)
        }
    }

    private fun dosePlan(regimen: ContraceptiveRegimen, date: Long): PlannedPillAlarm? {
        val number = calculator.pillNumber(regimen, date) ?: return null
        return PlannedPillAlarm(date, number, PillAlarmEvent.DOSE, calculator.doseTime(regimen, date))
    }
}
