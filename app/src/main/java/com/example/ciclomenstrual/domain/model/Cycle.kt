package com.example.ciclomenstrual.domain.model

data class Cycle(
    val startDate: Long,
    val endDate: Long? = null,
) {
    fun contains(date: Long): Boolean =
        date >= startDate && endDate?.let { date <= it } == true
}
