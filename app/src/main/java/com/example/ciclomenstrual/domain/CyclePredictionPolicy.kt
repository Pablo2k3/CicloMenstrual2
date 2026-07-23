package com.example.ciclomenstrual.domain

import com.example.ciclomenstrual.domain.model.Cycle

interface CyclePredictionPolicy {
    fun predict(cycle: Cycle): Long
}

class FixedCyclePredictionPolicy(
    private val cycleLengthDays: Int = 28,
) : CyclePredictionPolicy {
    override fun predict(cycle: Cycle): Long =
        DateNormalizer.addDays(cycle.startDate, cycleLengthDays)
}
