package com.example.ciclomenstrual.domain.repository

import com.example.ciclomenstrual.domain.model.ContraceptiveRegimen
import com.example.ciclomenstrual.domain.model.PillIntake

interface ContraceptiveRepository {
    suspend fun getRegimens(): List<ContraceptiveRegimen>
    suspend fun getActiveRegimen(): ContraceptiveRegimen?
    suspend fun getIntakes(): List<PillIntake>
    suspend fun startRegimen(regimen: ContraceptiveRegimen, initialIntakes: List<PillIntake>): Long
    suspend fun saveIntake(intake: PillIntake)
    suspend fun deleteIntake(regimenId: Long, scheduledDate: Long)
}
