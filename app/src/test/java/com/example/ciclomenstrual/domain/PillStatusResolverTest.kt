package com.example.ciclomenstrual.domain

import com.example.ciclomenstrual.domain.model.ContraceptiveRegimen
import com.example.ciclomenstrual.domain.model.PillDayStatus
import com.example.ciclomenstrual.domain.model.PillIntake
import com.example.ciclomenstrual.domain.model.PillIntakeSource
import com.example.ciclomenstrual.domain.model.PillIntakeStatus
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class PillStatusResolverTest {
    private val day = Calendar.getInstance(TimeZone.getTimeZone(DateNormalizer.REFERENCE_TIME_ZONE_ID)).apply {
        clear()
        set(2026, Calendar.JULY, 23, 0, 0, 0)
    }.timeInMillis.let {
        DateNormalizer.normalize(it, TimeZone.getTimeZone(DateNormalizer.REFERENCE_TIME_ZONE_ID))
    }
    private val regimen = ContraceptiveRegimen(id = 1, startDate = day)
    private val calculator = PillScheduleCalculator()
    private val resolver = PillStatusResolver(calculator)

    @Test fun `active pill changes from upcoming to pending and missed`() {
        val dose = calculator.doseTime(regimen, day)
        assertEquals(PillDayStatus.UPCOMING, resolver.resolve(regimen, day, null, dose - 1)!!.status)
        assertEquals(PillDayStatus.PENDING, resolver.resolve(regimen, day, null, dose)!!.status)
        assertEquals(
            PillDayStatus.MISSED,
            resolver.resolve(regimen, day, null, dose + PillStatusResolver.REMINDER_WINDOW_MILLIS)!!.status,
        )
    }

    @Test fun `stored taken state overrides time`() {
        val intake = PillIntake(
            1, day, 1, PillIntakeStatus.TAKEN, day, PillIntakeSource.CALENDAR,
        )
        assertEquals(
            PillDayStatus.TAKEN,
            resolver.resolve(regimen, day, intake, calculator.doseTime(regimen, day) + 99_999_999)!!.status,
        )
    }
}
