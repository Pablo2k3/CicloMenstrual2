package com.example.ciclomenstrual.domain

import com.example.ciclomenstrual.domain.model.ContraceptiveRegimen
import com.example.ciclomenstrual.domain.model.PillIntakeStatus
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PillAlarmPolicyTest {
    private val madrid = TimeZone.getTimeZone(DateNormalizer.REFERENCE_TIME_ZONE_ID)
    private val date = Calendar.getInstance(madrid).apply {
        clear()
        set(2026, Calendar.JULY, 23, 0, 0, 0)
    }.timeInMillis.let { DateNormalizer.normalize(it, madrid) }
    private val regimen = ContraceptiveRegimen(id = 4, startDate = date)
    private val calculator = PillScheduleCalculator()

    @Test
    fun `a delayed dose alarm remains valid during the active window`() {
        val dose = calculator.doseTime(regimen, date)

        assertTrue(
            PillAlarmPolicy.isProcessable(
                regimen,
                date,
                1,
                PillAlarmEvent.DOSE,
                dose + 5 * 60_000,
                null,
            ),
        )
    }

    @Test
    fun `an old reminder after the deadline is rejected`() {
        val deadline = calculator.doseTime(regimen, date) + PillStatusResolver.REMINDER_WINDOW_MILLIS

        assertFalse(
            PillAlarmPolicy.isProcessable(
                regimen,
                date,
                1,
                PillAlarmEvent.REMINDER,
                deadline + 1,
                null,
            ),
        )
    }

    @Test
    fun `a completed intake rejects a stale alarm`() {
        assertFalse(
            PillAlarmPolicy.isProcessable(
                regimen,
                date,
                1,
                PillAlarmEvent.DEADLINE,
                calculator.doseTime(regimen, date) + PillStatusResolver.REMINDER_WINDOW_MILLIS,
                PillIntakeStatus.TAKEN,
            ),
        )
    }

    @Test
    fun `notification action accepts only the current active pill`() {
        assertTrue(PillAlarmPolicy.isCurrentAction(regimen, date, 1, date))
        assertFalse(PillAlarmPolicy.isCurrentAction(regimen, DateNormalizer.addDays(date, -1), 1, date))
        assertFalse(PillAlarmPolicy.isCurrentAction(regimen, date, 22, date))
    }
}
