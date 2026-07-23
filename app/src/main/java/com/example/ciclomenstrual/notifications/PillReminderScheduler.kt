package com.example.ciclomenstrual.notifications

import com.example.ciclomenstrual.domain.model.ContraceptiveRegimen
import com.example.ciclomenstrual.domain.model.PillIntake

interface PillReminderScheduler {
    fun scheduleNext(regimen: ContraceptiveRegimen, intakes: List<PillIntake>, now: Long)
    fun cancel()
    fun canScheduleExact(): Boolean
}
