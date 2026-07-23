package com.example.ciclomenstrual.notifications

interface CycleReminderScheduler {
    fun schedule(predictedCycleStart: Long)
    fun cancel()
}
