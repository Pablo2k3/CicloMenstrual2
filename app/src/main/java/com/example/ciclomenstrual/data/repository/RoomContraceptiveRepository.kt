package com.example.ciclomenstrual.data.repository

import androidx.room.withTransaction
import com.example.ciclomenstrual.data.local.AppDatabase
import com.example.ciclomenstrual.data.local.RoomContraceptiveRegimen
import com.example.ciclomenstrual.data.local.RoomPillIntake
import com.example.ciclomenstrual.domain.DateNormalizer
import com.example.ciclomenstrual.domain.model.ContraceptiveRegimen
import com.example.ciclomenstrual.domain.model.PillIntake
import com.example.ciclomenstrual.domain.model.PillIntakeSource
import com.example.ciclomenstrual.domain.model.PillIntakeStatus
import com.example.ciclomenstrual.domain.repository.ContraceptiveRepository

class RoomContraceptiveRepository(
    private val database: AppDatabase,
) : ContraceptiveRepository {
    private val dao = database.contraceptiveDao()

    override suspend fun getRegimens(): List<ContraceptiveRegimen> =
        dao.getRegimens().map(::toDomain)

    override suspend fun getActiveRegimen(): ContraceptiveRegimen? =
        dao.getActiveRegimen()?.let(::toDomain)

    override suspend fun getIntakes(): List<PillIntake> =
        dao.getIntakes().map(::toDomain)

    override suspend fun startRegimen(
        regimen: ContraceptiveRegimen,
        initialIntakes: List<PillIntake>,
    ): Long = database.withTransaction {
        dao.closeActiveRegimen(DateNormalizer.addDays(regimen.startDate, -1))
        val id = dao.insertRegimen(regimen.toRoom())
        dao.insertIntakes(initialIntakes.map { it.copy(regimenId = id).toRoom() })
        id
    }

    override suspend fun saveIntake(intake: PillIntake) = dao.insertIntake(intake.toRoom())

    override suspend fun deleteIntake(regimenId: Long, scheduledDate: Long) =
        dao.deleteIntake(regimenId, scheduledDate)

    private fun toDomain(value: RoomContraceptiveRegimen) = ContraceptiveRegimen(
        id = value.id,
        startDate = value.startDate,
        endDate = value.endDate,
        doseHour = value.doseHour,
        doseMinute = value.doseMinute,
        activeDays = value.activeDays,
        placeboDays = value.placeboDays,
    )

    private fun toDomain(value: RoomPillIntake) = PillIntake(
        regimenId = value.regimenId,
        scheduledDate = value.scheduledDate,
        pillNumber = value.pillNumber,
        status = PillIntakeStatus.valueOf(value.status),
        takenAt = value.takenAt,
        source = PillIntakeSource.valueOf(value.source),
    )

    private fun ContraceptiveRegimen.toRoom() = RoomContraceptiveRegimen(
        id, startDate, endDate, doseHour, doseMinute, activeDays, placeboDays,
    )

    private fun PillIntake.toRoom() = RoomPillIntake(
        regimenId, scheduledDate, pillNumber, status.name, takenAt, source.name,
    )
}
