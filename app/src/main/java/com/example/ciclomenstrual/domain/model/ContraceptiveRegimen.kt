package com.example.ciclomenstrual.domain.model

data class ContraceptiveRegimen(
    val id: Long = 0,
    val startDate: Long,
    val endDate: Long? = null,
    val doseHour: Int = 14,
    val doseMinute: Int = 0,
    val activeDays: Int = 21,
    val placeboDays: Int = 7,
) {
    val packLength: Int get() = activeDays + placeboDays
}
