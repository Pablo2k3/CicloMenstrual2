package com.example.ciclomenstrual.data.repository

import com.example.ciclomenstrual.data.local.CycleDao
import com.example.ciclomenstrual.data.local.RoomCycle
import com.example.ciclomenstrual.domain.model.Cycle
import com.example.ciclomenstrual.domain.repository.CycleRepository

class RoomCycleRepository(private val dao: CycleDao) : CycleRepository {
    override suspend fun getAll(): List<Cycle> =
        dao.getAll().map { Cycle(it.startDate, it.endDate.takeUnless { value -> value == 0L }) }

    override suspend fun insert(cycle: Cycle) =
        dao.insert(RoomCycle(cycle.startDate, cycle.endDate ?: 0))

    override suspend fun updateEnd(startDate: Long, endDate: Long) =
        dao.updateEnd(startDate, endDate)

    override suspend fun delete(cycle: Cycle) =
        dao.delete(RoomCycle(cycle.startDate, cycle.endDate ?: 0))

    override suspend fun deleteByStartDate(startDate: Long) = dao.deleteByStartDate(startDate)
    override suspend fun exists(startDate: Long): Boolean = dao.exists(startDate)
}
