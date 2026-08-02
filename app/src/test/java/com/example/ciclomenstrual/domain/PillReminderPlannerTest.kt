package com.example.ciclomenstrual.domain

import com.example.ciclomenstrual.domain.model.ContraceptiveRegimen
import com.example.ciclomenstrual.domain.model.PillIntake
import com.example.ciclomenstrual.domain.model.PillIntakeSource
import com.example.ciclomenstrual.domain.model.PillIntakeStatus
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class PillReminderPlannerTest {
    private val day = Calendar.getInstance(TimeZone.getTimeZone(DateNormalizer.REFERENCE_TIME_ZONE_ID)).apply {
        clear()
        set(2026, Calendar.JULY, 23, 0, 0, 0)
    }.timeInMillis.let {
        DateNormalizer.normalize(it, TimeZone.getTimeZone(DateNormalizer.REFERENCE_TIME_ZONE_ID))
    }
    private val regimen = ContraceptiveRegimen(id = 1, startDate = day)
    private val calculator = PillScheduleCalculator()
    private val planner = PillReminderPlanner(calculator)

    @Test fun `first alarm is at fourteen hundred`() {
        val dose = calculator.doseTime(regimen, day)
        val plan = planner.next(regimen, emptyList(), dose - 60_000)!!
        assertEquals(PillAlarmEvent.DOSE, plan.event)
        assertEquals(dose, plan.triggerAt)
    }

    @Test fun `last reminder is fifteen forty five then deadline at sixteen`() {
        val dose = calculator.doseTime(regimen, day)
        val at1530 = planner.next(regimen, emptyList(), dose + 90 * 60_000)!!
        assertEquals(PillAlarmEvent.REMINDER, at1530.event)
        assertEquals(dose + 105 * 60_000, at1530.triggerAt)

        val at1545 = planner.next(regimen, emptyList(), dose + 105 * 60_000)!!
        assertEquals(PillAlarmEvent.DEADLINE, at1545.event)
        assertEquals(dose + 120 * 60_000, at1545.triggerAt)
    }

    @Test fun `taken pill schedules next day`() {
        val dose = calculator.doseTime(regimen, day)
        val intake = PillIntake(
            1, day, 1, PillIntakeStatus.TAKEN, dose, PillIntakeSource.NOTIFICATION,
        )
        val plan = planner.next(regimen, listOf(intake), dose + 1)!!
        assertEquals(DateNormalizer.addDays(day, 1), plan.date)
        assertEquals(PillAlarmEvent.DOSE, plan.event)
    }
}
